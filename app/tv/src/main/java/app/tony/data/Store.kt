package app.tony.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.ds by preferencesDataStore("tony")

/** Everything Tony remembers lives in app-private storage; uninstalling removes it all. */
data class Saved(
    val firstRunDone: Boolean = false,
    val countries: List<String> = emptyList(),      // country files in row order, e.g. "countries/lebanon.json"
    val favourites: List<Channel> = emptyList(),
    val recent: List<Channel> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val subtitles: Boolean = true,       // automatic: on whenever a channel has them
    val captionSize: Int = 1,
    val captionStyle: Int = 0,
)

class Store(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val chList = ListSerializer(Channel.serializer())
    private val strList = ListSerializer(String.serializer())

    private object K {
        val firstRun = booleanPreferencesKey("firstRunDone")
        val countries = stringPreferencesKey("countries")
        val favourites = stringPreferencesKey("favourites")
        val recent = stringPreferencesKey("recent")
        val hidden = stringPreferencesKey("hidden")
        val subtitles = booleanPreferencesKey("subtitles")
        val captionSize = androidx.datastore.preferences.core.intPreferencesKey("captionSize")
        val captionStyle = androidx.datastore.preferences.core.intPreferencesKey("captionStyle")
    }

    private fun <T> decode(s: String?, d: kotlinx.serialization.DeserializationStrategy<List<T>>): List<T> =
        s?.let { runCatching { json.decodeFromString(d, it) }.getOrNull() } ?: emptyList()

    val saved: Flow<Saved> = context.ds.data.map { p ->
        Saved(
            firstRunDone = p[K.firstRun] ?: false,
            countries = decode(p[K.countries], strList),
            favourites = decode(p[K.favourites], chList),
            recent = decode(p[K.recent], chList),
            hidden = decode(p[K.hidden], strList).toSet(),
            subtitles = p[K.subtitles] ?: true,
            captionSize = p[K.captionSize] ?: 1,
            captionStyle = p[K.captionStyle] ?: 0,
        )
    }

    suspend fun setCountries(files: List<String>) = context.ds.edit {
        it[K.countries] = json.encodeToString(strList, files.distinct())
        it[K.firstRun] = true
    }

    suspend fun setFavourites(list: List<Channel>) = context.ds.edit { it[K.favourites] = json.encodeToString(chList, list.distinctBy { c -> c.id }) }

    suspend fun pushRecent(c: Channel) = context.ds.edit { p ->
        val cur = decode(p[K.recent], chList).filterNot { it.id == c.id }
        p[K.recent] = json.encodeToString(chList, (listOf(c) + cur).take(20))
    }

    suspend fun setHidden(ids: Set<String>) = context.ds.edit { it[K.hidden] = json.encodeToString(strList, ids.toList()) }
    suspend fun setSubtitles(on: Boolean) = context.ds.edit { it[K.subtitles] = on }
    suspend fun setCaptionLook(size: Int, style: Int) = context.ds.edit { it[K.captionSize] = size; it[K.captionStyle] = style }
}
