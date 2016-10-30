package eu.kanade.tachiyomi.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.tables.*

class DbOpenHelper(context: Context)
: SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.db"

        /**
         * Version of the database.
         */
        const val DATABASE_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase) = with(db) {
        execSQL(MangaTable.createTableQuery)
        execSQL(ChapterTable.createTableQuery)
        execSQL(MangaSyncTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        execSQL(MangaCategoryTable.createTableQuery)
        execSQL(HistoryTable.createTableQuery)

        // DB indexes
        execSQL(MangaTable.createUrlIndexQuery)
        execSQL(MangaTable.createFavoriteIndexQuery)
        execSQL(ChapterTable.createMangaIdIndexQuery)
        execSQL(HistoryTable.createChapterIdIndexQuery)

        // DB triggers
        execSQL(MangaTable.createLastModifiedTriggerQuery)
        execSQL(ChapterTable.createLastModifiedTriggerQuery)
        execSQL(CategoryTable.createLastModifiedTriggerQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(ChapterTable.sourceOrderUpdateQuery)

            // Fix kissmanga covers after supporting cloudflare
            db.execSQL("""UPDATE mangas SET thumbnail_url =
                    REPLACE(thumbnail_url, '93.174.95.110', 'kissmanga.com') WHERE source = 4""")
        }
        if (oldVersion < 3) {
            // Initialize history tables
            db.execSQL(HistoryTable.createTableQuery)
            db.execSQL(HistoryTable.createChapterIdIndexQuery)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

}
