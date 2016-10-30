package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Manga : Serializable {

    var id: Long?

    var source: Int

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: String?

    var status: Int

    var thumbnail_url: String?

    var favorite: Boolean

    var last_update: Long

    var initialized: Boolean

    var viewer: Int

    var chapter_flags: Int

    var unread: Int

    var category: Int

    var last_modified: Long

    fun copyFrom(other: Manga) {
        if (other.author != null)
            author = other.author

        if (other.artist != null)
            artist = other.artist

        if (other.description != null)
            description = other.description

        if (other.genre != null)
            genre = other.genre

        if (other.thumbnail_url != null)
            thumbnail_url = other.thumbnail_url

        status = other.status

        initialized = true
    }

    fun setChapterOrder(order: Int) {
        setFlags(order, SORT_MASK)
    }

    private fun setFlags(flag: Int, mask: Int) {
        chapter_flags = chapter_flags and mask.inv() or (flag and mask)
    }

    fun sortDescending(): Boolean {
        return chapter_flags and SORT_MASK == SORT_DESC
    }

    // Used to display the chapter's title one way or another
    var displayMode: Int
        get() = chapter_flags and DISPLAY_MASK
        set(mode) = setFlags(mode, DISPLAY_MASK)

    var readFilter: Int
        get() = chapter_flags and READ_MASK
        set(filter) = setFlags(filter, READ_MASK)

    var downloadedFilter: Int
        get() = chapter_flags and DOWNLOADED_MASK
        set(filter) = setFlags(filter, DOWNLOADED_MASK)

    var sorting: Int
        get() = chapter_flags and SORTING_MASK
        set(sort) = setFlags(sort, SORTING_MASK)

    companion object {

        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3

        const val SORT_DESC = 0x00000000
        const val SORT_ASC = 0x00000001
        const val SORT_MASK = 0x00000001

        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000

        const val SHOW_UNREAD = 0x00000002
        const val SHOW_READ = 0x00000004
        const val READ_MASK = 0x00000006

        const val SHOW_DOWNLOADED = 0x00000008
        const val SHOW_NOT_DOWNLOADED = 0x00000010
        const val DOWNLOADED_MASK = 0x00000018

        const val SORTING_SOURCE = 0x00000000
        const val SORTING_NUMBER = 0x00000100
        const val SORTING_MASK = 0x00000100

        const val DISPLAY_NAME = 0x00000000
        const val DISPLAY_NUMBER = 0x00100000
        const val DISPLAY_MASK = 0x00100000

        fun create(source: Int): Manga = MangaImpl().apply {
            this.source = source
        }

        fun create(pathUrl: String, source: Int = 0): Manga = MangaImpl().apply {
            url = pathUrl
            this.source = source
        }
    }

}