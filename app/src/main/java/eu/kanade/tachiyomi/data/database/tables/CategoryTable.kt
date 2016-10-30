package eu.kanade.tachiyomi.data.database.tables

object CategoryTable {

    const val TABLE = "categories"

    const val COL_ID = "_id"

    const val COL_NAME = "name"

    const val COL_ORDER = "sort"

    const val COL_FLAGS = "flags"

    const val COL_LAST_MODIFIED = "cat_last_modified"

    const val TRIGGER_LAST_MODIFIED = "cat_last_modified_trigger"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_NAME TEXT NOT NULL,
            $COL_ORDER INTEGER NOT NULL,
            $COL_FLAGS INTEGER NOT NULL,
            $COL_LAST_MODIFIED LONG NOT NULL DEFAULT (strftime('%s', 'now')*1000)
            )"""

    val createLastModifiedTriggerQuery: String
        get() = """CREATE TRIGGER IF NOT EXISTS $TRIGGER_LAST_MODIFIED
                AFTER UPDATE ON $TABLE FOR EACH ROW
            BEGIN
                UPDATE $TABLE
                    SET $COL_LAST_MODIFIED = strftime('%s', 'now')*1000
                    WHERE $COL_ID = old.$COL_ID;
            END"""
}
