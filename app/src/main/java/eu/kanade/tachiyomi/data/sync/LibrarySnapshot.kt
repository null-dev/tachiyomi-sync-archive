package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import java.util.ArrayList

/**
 * Snapshot of library state recorded immediately after a successful sync.
 */

class LibrarySnapshot(val timestamp: Long = System.currentTimeMillis(),
                      val last_history: Long) {
    val categories = ArrayList<Int>()
    val chapters = ArrayList<Long>()
    val manga = ArrayList<Long>()
    val category_mappings = ArrayList<MangaCategory>()

    companion object {
        fun fromDb(db: DatabaseHelper): LibrarySnapshot {
            val snapshot = LibrarySnapshot(last_history = db.getLastHistoryId().executeAsBlocking() ?: -1)
            //Add categories
            snapshot.categories.addAll(db.getCategories().executeAsBlocking().map { it.id!! })
            snapshot.chapters.addAll(db.getAllChapters().executeAsBlocking().map { it.id!! })
            snapshot.manga.addAll(db.getMangas().executeAsBlocking().map { it.id!! })
            snapshot.category_mappings.addAll(db.getAllMangaCategories().executeAsBlocking())
            return snapshot
        }
    }
}