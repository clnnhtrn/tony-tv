package app.tony.data

import kotlinx.serialization.Serializable

/* Shapes of the files in channels/dist/ (see channels/README.md). Unknown fields are ignored. */

@Serializable
data class Index(
    val schema: Int = 1,
    val generated: String? = null,
    val countries: List<CountryEntry> = emptyList(),
    val rows: List<RowEntry> = emptyList(),
)

@Serializable
data class CountryEntry(
    val name: String,
    val code: String? = null,
    val kind: String = "country",
    val flag: String = "",
    val file: String,
    val channels: Int = 0,
    val working: Int = 0,
)

@Serializable
data class RowEntry(val key: String, val title: String, val file: String, val channels: Int = 0)

@Serializable
data class ChannelFile(val country: String? = null, val row: String? = null, val code: String? = null, val generated: String? = null, val channels: List<Channel> = emptyList())

@Serializable
data class Channel(
    val id: String,
    val number: Int = 0,
    val name: String,
    val country: String? = null,
    val languages: List<String> = emptyList(),
    val category: String? = null,
    val url: String,
    val headers: Map<String, String>? = null,
    val backupUrls: List<String> = emptyList(),
    val status: String = "untested",
    val media: String = "video",
    val hd: Boolean = false,
    val geoblocked: Boolean = false,
    val notAlwaysOn: Boolean = false,
    val logo: String? = null,
    val logoBadge: String? = null,
    val logoBadgeColor: String? = null,
    val tileColor: String? = null,
    val logoWidth: Int? = null,
    val logoHeight: Int? = null,
) {
    val isYouTube get() = media == "youtube"
    val isWebOnly get() = media == "web" || status == "web"
    val isRadio get() = media == "audio"
    val isWorking get() = status == "ok"
}

@Serializable
data class FilmsIndex(val total: Int = 0, val all: String? = null, val rows: List<FilmRowEntry> = emptyList(), val generated: String? = null)

@Serializable
data class FilmRowEntry(val key: String, val title: String, val file: String, val films: Int = 0)

@Serializable
data class FilmsFile(val row: String? = null, val films: List<Film> = emptyList())

@Serializable
data class Film(
    val id: String,
    val title: String,
    val year: Int? = null,
    val minutes: Int? = null,
    val description: String = "",
    val poster: String? = null,
    val url: String,
    val source: String = "Internet Archive",
    val page: String? = null,
)
