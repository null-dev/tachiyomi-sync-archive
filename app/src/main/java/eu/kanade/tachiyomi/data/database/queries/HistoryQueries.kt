package eu.kanade.tachiyomi.data.database.queries

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.database.resolvers.HistoryLastReadPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaChapterHistoryGetResolver
import eu.kanade.tachiyomi.data.database.tables.HistoryTable
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.TABLE
import java.util.*

interface HistoryQueries : DbProvider {

    /**
     * Insert history into database
     * @param history object containing history information
     */
    fun insertHistory(history: History) = db.put().`object`(history).prepare()

    /**
     * Returns history of recent manga containing last read chapter
     * @param date recent date range
     */
    fun getRecentManga(date: Date) = db.get()
            .listOfObjects(MangaChapterHistory::class.java)
            .withQuery(RawQuery.builder()
                    .query(getRecentMangasQuery())
                    .args(date.time)
                    .observesTables(TABLE)
                    .build())
            .withGetResolver(MangaChapterHistoryGetResolver.INSTANCE)
            .prepare()

    fun getHistoryByMangaId(mangaId: Long) = db.get()
            .listOfObjects(History::class.java)
            .withQuery(RawQuery.builder()
                    .query(getHistoryByMangaId())
                    .args(mangaId)
                    .observesTables(TABLE)
                    .build())
            .prepare()

    fun getLastHistoryId() = db.get().`object`(Long::class.java)
            .withQuery(RawQuery.builder()
                    .query("SELECT $COL_ID FROM $TABLE WHERE $COL_ID = (SELECT MAX($COL_ID) FROM $TABLE)")
                    .build())
            .withGetResolver(object: DefaultGetResolver<Long>() {
                override fun mapFromCursor(cursor: Cursor): Long {
                    return cursor.getLong(0)
                }
            })
            .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param history history object
     */
    fun updateHistoryLastRead(history: History) = db.put()
            .`object`(history)
            .withPutResolver(HistoryLastReadPutResolver())
            .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param historyList history object list
     */
    fun updateHistoryLastRead(historyList: List<History>) = db.put()
            .objects(historyList)
            .withPutResolver(HistoryLastReadPutResolver())
            .prepare()
}
