package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import java.util.*

class LibraryDiff {
    val modifiedCategories = ArrayList<Category>()
    val removedCategories = ArrayList<String>()
    val modifiedChapters = ArrayList<Pair<MangaReference, Chapter>>()
    val modifiedManga = ArrayList<Manga>()
    val addedMangaCategoryMappings = ArrayList<Pair<String, MangaReference>>()
    val removedMangaCategoryMappings = ArrayList<Pair<String, MangaReference>>()

    class MangaReference(val source: Int,
                         val url: String) {
        companion object {
            fun fromManga(manga: Manga): MangaReference
                    = MangaReference(manga.source, manga.url)
        }
    }
}