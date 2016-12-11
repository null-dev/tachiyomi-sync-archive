package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import java.util.ArrayList

/**
 * Snapshot of library state recorded immediately after a successful sync.
 */

class LibrarySnapshot(val timestamp: Long = System.currentTimeMillis()) {
    val categories = ArrayList<CategoryImpl>()

    val favManga = ArrayList<Long>()

    val category_mappings = ArrayList<MangaCategory>()

    companion object {
        fun fromDb(db: DatabaseHelper): LibrarySnapshot {
            val snapshot = LibrarySnapshot()
            //Add categories
            snapshot.categories.addAll(db.getCategories().executeAsBlocking().map { it as CategoryImpl })
            snapshot.favManga.addAll(db.getMangas().executeAsBlocking().filter { it.favorite }.map { it.id!! })
            snapshot.category_mappings.addAll(db.getAllMangaCategories().executeAsBlocking())
            return snapshot
        }

        fun empty() = LibrarySnapshot(0)
    }
}