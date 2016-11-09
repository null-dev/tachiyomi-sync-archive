package eu.kanade.tachiyomi.data.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import uy.kohesive.injekt.injectLazy

/**
 * Created by nulldev on 06/11/16.
 */

class LibrarySyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true, false) {

    val db: DatabaseHelper by injectLazy()

    override fun onPerformSync(account: Account?, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {
        val snapshot = LibrarySnapshot.fromDb(db) //TODO Get from previous sync
        val diff = DiffGenerator().generate(snapshot)
        //TODO Upload diff and download changes
    }
}