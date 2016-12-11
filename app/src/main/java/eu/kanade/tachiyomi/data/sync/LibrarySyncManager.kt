package eu.kanade.tachiyomi.data.sync

import android.content.Context
import java.io.File

/**
 * Created by nulldev on 09/11/16.
 */
class LibrarySyncManager(val context: Context) {

    val snapshotFile = File(context.filesDir, SNAPSHOT_FILENAME)

    companion object {
        val SNAPSHOT_FILENAME = "sync_snapshot.json"
    }
}