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

sealed interface Screen {
    data object Welcome : Screen
    data object Home : Screen
    data object Movies : Screen
    data class Category(val name: String) : Screen
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

    fun go(s: Screen) { if (stack.lastOrNull() != s) stack.add(s) }
    fun replaceTop(s: Screen) { if (stack.isNotEmpty()) stack[stack.lastIndex] = s else stack.add(s) }
    fun tab(s: Screen) { stack.clear(); stack.add(Screen.Home); if (s != Screen.Home) stack.add(s) }
    /** @return false when there is nothing to go back to (the system then leaves the app). */
    fun back(): Boolean { if (stack.size <= 1) return false; stack.removeAt(stack.lastIndex); return true }

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
        countryRowsAll = rows.map { (e, list) -> ChannelRow(e.file, e.name, null, list.sortedBy { it.number }, e.channels) }
        countryRows = rows.map { (e, list) ->
            ChannelRow(e.file, e.name, null, list.filterNot { it.id in saved.hidden }.sortedBy { it.number }, e.channels)
        }
        val inCountryRows = countryRows.flatMap { it.channels }.map { it.id }.toSet()
        extraRows = idx.rows.map { r -> viewModelScope.async {
            // Skip channels that already have their own country row (e.g. Lebanon in the Arabic row)
            val list = repo.channels(r.file).getOrNull()?.channels.orEmpty().filterNot { it.id in saved.hidden || it.id in inCountryRows }
            ChannelRow(r.key, r.title, when (r.key) { "movies" -> "Free movie channels, playing now"; "arabic" -> "Arabic channels from every country"; else -> null }, list.take(60), r.channels)
        } }.awaitAll().filter { it.channels.isNotEmpty() }
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
    }.sortedBy { if (it.isWorking) 0 else 1 }

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
    }

    fun showToast(text: String) {
        toastJob?.cancel()
        toast = text
        toastJob = viewModelScope.launch { delay(2200); toast = null }
    }

    /** Every channel we have loaded, for search and for "More from…". */
    fun allChannels(): List<Channel> = (countryRows.flatMap { it.channels } + extraRows.flatMap { it.channels }).distinctBy { it.id }

    fun countryEntries(): List<CountryEntry> = index?.countries.orEmpty().filter { it.kind == "country" }
}
