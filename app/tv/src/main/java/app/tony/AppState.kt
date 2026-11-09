package app.tony

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tony.data.Channel
import app.tony.data.CountryEntry
import app.tony.data.Index
import app.tony.data.Repository
import app.tony.data.Saved
import app.tony.data.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

sealed interface Screen {
    data object Welcome : Screen
    data object Home : Screen
    data object Movies : Screen
    data class Category(val name: String) : Screen
    data class Channels(val country: String? = null) : Screen
    data object Favourites : Screen
    data object Search : Screen
    data object Settings : Screen
    data class Details(val channel: Channel) : Screen
    data class Watch(val channel: Channel, val list: List<Channel>) : Screen
}

data class ChannelRow(val key: String, val title: String, val subtitle: String?, val channels: List<Channel>, val total: Int)

private val MAJORS = mapOf(
    "Lebanon" to listOf("mtv", "lbc", "jadeed", "otv", "tele liban", "mayadeen", "nbn", "manar", "voice of lebanon"),
    "Australia" to listOf("abc", "seven", "nine", "10", "sbs", "7two", "9gem"),
)

class AppState(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)
    private val store = Store(app)

    var saved by mutableStateOf(Saved()); private set
    /** True while Home/Movies is scrolled into the rows: the fixed top bar steps out of the way. */
    var pageScrolled by mutableStateOf(false)
    var index by mutableStateOf<Index?>(null); private set
    var loading by mutableStateOf(true); private set
    var offline by mutableStateOf(false); private set
    var countryRows by mutableStateOf<List<ChannelRow>>(emptyList()); private set
    var countryRowsAll by mutableStateOf<List<ChannelRow>>(emptyList()); private set   // incl. hidden (Settings)
    var extraRows by mutableStateOf<List<ChannelRow>>(emptyList()); private set   // Arabic, Movies
    var toast by mutableStateOf<String?>(null); private set
    val stack = mutableStateListOf<Screen>()
    private var toastJob: Job? = null
    var ready by mutableStateOf(false); private set

    val screen: Screen get() = stack.lastOrNull() ?: Screen.Home

    init {
        viewModelScope.launch {
            saved = store.saved.first()
            stack.add(if (saved.firstRunDone && saved.countries.isNotEmpty()) Screen.Home else Screen.Welcome)
            ready = true
            launch { store.saved.collect { saved = it } }
            reload()
        }
    }

    /** Open panels (options, channel strip, menus) register here; Back closes the newest first. */
    val backLayers = mutableStateListOf<() -> Unit>()

    /** One rule for the Back button: close the top panel, else go back a screen. False = leave Tony. */
    fun handleBack(): Boolean {
        backLayers.lastOrNull()?.let { it(); return true }
        return back()
    }

    fun go(s: Screen) { flushHere(); if (stack.lastOrNull() != s) stack.add(s) }
    fun replaceTop(s: Screen) { flushHere(); if (stack.isNotEmpty()) stack[stack.lastIndex] = s else stack.add(s) }
    fun tab(s: Screen) { flushHere(); stack.clear(); stack.add(Screen.Home); if (s != Screen.Home) stack.add(s) }
    /** @return false when there is nothing to go back to (the system then leaves the app). */
    fun back(): Boolean { flushHere(); if (stack.size <= 1) return false; stack.removeAt(stack.lastIndex); return true }

    fun reload() = viewModelScope.launch {
        lastLoad = System.currentTimeMillis()
        loading = true
        val idx = repo.index()
        offline = idx.isFailure
        index = idx.getOrNull()
        loadRows()
        loading = false
    }

    private suspend fun loadRows() {
        val idx = index ?: return
        val chosen = saved.countries
        val entries = chosen.mapNotNull { f -> idx.countries.firstOrNull { it.file == f } }
        val rows = viewModelScope.async {
            entries.map { e -> viewModelScope.async { e to repo.channels(e.file).getOrNull()?.channels.orEmpty() } }.awaitAll()
        }.await()
        rawCountry = rows.map { (e, list) -> ChannelRow(e.file, e.name, null, list.sortedBy { it.number }, e.channels) }
        val inCountryRows = rows.flatMap { (_, list) -> list.filterNot { it.id in saved.hidden } }.map { it.id }.toSet()
        // Collections (e.g. Arabic) only when the viewer chose them; Movies always (it has its own tab)
        rawExtra = idx.rows.filter { it.key == "movies" || it.file in saved.countries }.map { r -> viewModelScope.async {
            // Skip channels that already have their own country row (e.g. Lebanon in the Arabic row)
            val list = repo.channels(r.file).getOrNull()?.channels.orEmpty().filterNot { it.id in saved.hidden || it.id in inCountryRows }
            ChannelRow(r.key, r.title, null, list, r.channels)
        } }.awaitAll().filter { it.channels.isNotEmpty() }
        rebuild()
    }

    // ---- does it play on THIS TV? ----
    // The server checks from the USA; what counts for the family is their own TV. Tony remembers each channel's
    // result here (app-private), re-checks while it's open, and that beats the server's view for a day.
    private var rawCountry: List<ChannelRow> = emptyList()
    private var rawExtra: List<ChannelRow> = emptyList()
    private val herePrefs = app.getSharedPreferences("here", android.content.Context.MODE_PRIVATE)
    private val here = HashMap<String, Pair<String, Long>>().apply {
        herePrefs.all.forEach { (k, v) -> (v as? String)?.split("|")?.takeIf { it.size == 2 }?.let { put(k, it[0] to (it[1].toLongOrNull() ?: 0L)) } }
    }
    private var herePending = false

    private fun eff(c: Channel): Channel {
        val (st, at) = here[c.id] ?: return c
        if (System.currentTimeMillis() - at > 24 * 3600_000L || st == c.status) return c
        // a fresh "ok" here beats the server; a fresh failure here beats a server "ok"
        return c.copy(status = st)
    }

    /** A saved copy (favourite, recent) with today's status for this TV. */
    fun current(c: Channel): Channel =
        (countryRowsAll.asSequence().flatMap { it.channels } + extraRows.asSequence().flatMap { it.channels }).firstOrNull { it.id == c.id } ?: eff(c)

    private fun downLast(l: List<Channel>) = l.sortedBy { if (it.isDown) 1 else 0 }   // stable: keeps the order otherwise

    /** Apply this TV's results to every row. Runs on screen changes only, so cards never jump under the cursor. */
    private fun rebuild() {
        herePending = false
        countryRowsAll = rawCountry.map { r -> r.copy(channels = r.channels.map(::eff)) }
        countryRows = countryRowsAll.map { r -> r.copy(channels = r.channels.filterNot { it.id in saved.hidden }) }
        extraRows = rawExtra.map { r -> r.copy(channels = downLast(r.channels.map(::eff))) }
    }

    private fun flushHere() { if (herePending) rebuild() }

    fun markHere(c: Channel, status: String) {
        val now = System.currentTimeMillis()
        val prev = here[c.id]
        if (prev != null && prev.first == status && now - prev.second < 10 * 60_000L) return
        here[c.id] = status to now
        herePrefs.edit().putString(c.id, "$status|$now").apply()
        if (eff(c).status != (countryRowsAll.flatMap { it.channels } + extraRows.flatMap { it.channels }).firstOrNull { it.id == c.id }?.status) herePending = true
    }

    /** Re-check channels from this TV: favourites, recent and the chosen countries. Only while Tony is open. */
    private suspend fun checkHere() {
        val list = (saved.favourites + saved.recent + rawCountry.flatMap { it.channels })
            .distinctBy { it.id }.filter { !it.isYouTube && !it.isWebOnly && it.id !in saved.hidden }.take(250)
        val gate = kotlinx.coroutines.sync.Semaphore(5)
        coroutineScope {
            list.map { c -> async {
                gate.acquire()
                try {
                    if (!foreground) return@async
                    var result = "broken"
                    for (u in listOf(c.url) + c.backupUrls) {
                        val r = repo.probe(u, c.headers)
                        if (r == "ok") { result = "ok"; break }
                        if (r == "geoblocked") result = "geoblocked"
                    }
                    markHere(c, result)
                } finally { gate.release() }
            } }.awaitAll()
        }
    }

    // ---- choices ----
    fun chooseCountries(files: List<String>) = viewModelScope.launch {
        store.setCountries(files)
        saved = saved.copy(countries = files, firstRunDone = true)
        if (screen == Screen.Welcome) replaceTop(Screen.Home)
        loadRows()
    }

    fun isFavourite(c: Channel) = saved.favourites.any { it.id == c.id }

    fun toggleFavourite(c: Channel) = viewModelScope.launch {
        val fav = isFavourite(c)
        store.setFavourites(if (fav) saved.favourites.filterNot { it.id == c.id } else saved.favourites + c)
        showToast(if (fav) "Removed from Favourites" else "Added to Favourites")
    }

    fun moveFavourite(c: Channel, by: Int) = viewModelScope.launch {
        val l = saved.favourites.toMutableList()
        val i = l.indexOfFirst { it.id == c.id }
        val j = (i + by).coerceIn(0, l.lastIndex)
        if (i < 0 || i == j) return@launch
        l.add(j, l.removeAt(i)); store.setFavourites(l)
    }

    fun watched(c: Channel) = viewModelScope.launch { store.pushRecent(c) }
    fun setHidden(id: String, hide: Boolean) = viewModelScope.launch {
        store.setHidden(if (hide) saved.hidden + id else saved.hidden - id); loadRows()
    }
    fun setSubtitles(on: Boolean) = viewModelScope.launch { store.setSubtitles(on) }
    fun addWatchTime(id: String, ms: Long) = viewModelScope.launch { store.addWatchTime(id, ms) }

    /**
     * "Top in <country>": what this TV watches most, then the country's main broadcasters, then list order.
     * Working channels only.
     */
    fun topFor(r: ChannelRow, n: Int = 10): List<Channel> {
        val majors = MAJORS[r.title].orEmpty()
        fun majorRank(c: Channel) = majors.indexOfFirst { c.name.lowercase().contains(it) }.let { if (it < 0) 999 else it }
        return r.channels.filter { it.isWorking }
            .sortedWith(compareByDescending<Channel> { saved.watchTime[it.id] ?: 0L }.thenBy { majorRank(it) }.thenBy { it.number })
            .take(n)
    }

    /** Channels for a category tile (News, Movies, Kids, Music, Sport, Arabic). */
    fun category(name: String): List<Channel> = allChannels().filter {
        when (name) {
            "Arabic" -> "ara" in it.languages
            "Sport" -> it.category == "Sport"
            else -> it.category == name
        }
    }.sortedBy { if (it.isDown) 1 else 0 }

    fun setCaptionLook(size: Int, style: Int) = viewModelScope.launch { store.setCaptionLook(size, style) }

    /** The list refreshes by itself: on open, then every 30 minutes while Tony is on screen (never in the background). */
    var foreground = false
    private var lastLoad = 0L
    fun onForeground() {
        foreground = true
        if (System.currentTimeMillis() - lastLoad > 30 * 60_000L && index != null) reload()
    }
    init {
        viewModelScope.launch { while (true) { delay(30 * 60_000L); if (foreground) reload() } }
        // This TV's own channel check: a minute after opening, then every 20 minutes (only while Tony is open)
        viewModelScope.launch { delay(60_000L); while (true) { if (foreground && rawCountry.isNotEmpty()) runCatching { checkHere() }; delay(20 * 60_000L) } }
    }

    fun showToast(text: String) {
        toastJob?.cancel()
        toast = text
        toastJob = viewModelScope.launch { delay(2200); toast = null }
    }

    /** Every channel we have loaded, for search and for "More from…". */
    /** Up/Down while watching: the channel's own country in channel-number order (like a TV), wherever it was opened from. */
    fun zapList(c: Channel, fallback: List<Channel>): List<Channel> =
        countryRows.firstOrNull { r -> r.channels.any { it.id == c.id } }?.channels
            ?: fallback.takeIf { l -> l.any { it.id == c.id } }?.sortedBy { if (it.number > 0) it.number else Int.MAX_VALUE }
            ?: listOf(c)

    /** "More like this": same kind of channel first (category), then same language, then same country; working ones first. */
    fun similar(c: Channel, n: Int = 30): List<Channel> {
        val cat = c.category?.lowercase()?.takeIf { it.isNotBlank() }
        val broad = cat == null || cat == "general"
        return allChannels().asSequence().filter { it.id != c.id && !it.isRadio == !c.isRadio }
            .map { o ->
                var score = 0
                if (cat != null && o.category?.lowercase() == cat) score += if (broad) 2 else 6
                if (o.languages.any { it in c.languages }) score += 3
                if (o.country == c.country) score += 2
                if (o.isWorking) score += 1
                o to score
            }
            .filter { it.second >= 3 }
            .sortedWith(compareBy<Pair<Channel, Int>> { if (it.first.isDown) 1 else 0 }.thenByDescending { it.second }.thenBy { if (it.first.number > 0) it.first.number else Int.MAX_VALUE })
            .map { it.first }.take(n).toList()
    }

    fun allChannels(): List<Channel> = (countryRows.flatMap { it.channels } + extraRows.flatMap { it.channels }).distinctBy { it.id }

    fun countryEntries(): List<CountryEntry> = index?.countries.orEmpty().filter { it.kind == "country" }

    /** Pickable collections that aren't a country (e.g. "Arabic": Arabic-language channels from every country). */
    fun collectionEntries(): List<CountryEntry> = index?.rows.orEmpty().filter { it.key != "movies" }.map {
        CountryEntry(name = it.title, code = null, kind = "collection", flag = "", file = it.file, channels = it.channels, working = it.channels)
    }

    /** Countries suggested on first run: the TV's own region (works for any family, not just one country). */
    fun suggestedCountries(): List<String> {
        val region = java.util.Locale.getDefault().country.uppercase()
        val lang = java.util.Locale.getDefault().language
        val byRegion = countryEntries().filter { it.code == region }.map { it.name }
        val byLanguage = when (lang) { "ar" -> listOf("Lebanon", "Egypt", "Saudi Arabia"); "vi" -> listOf("Vietnam"); else -> emptyList() }
        return (byRegion + byLanguage).distinct()
    }
}
