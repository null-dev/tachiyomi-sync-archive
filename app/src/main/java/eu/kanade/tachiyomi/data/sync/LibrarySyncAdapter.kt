package eu.kanade.tachiyomi.data.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.JsonElement
import eu.kanade.tachiyomi.data.backup.BackupManager
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.sync.account.SyncAccountAuthenticator
import eu.kanade.tachiyomi.data.sync.api.TWApi
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.IOException

/**
 * Created by nulldev on 06/11/16.
 */

class LibrarySyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true, false) {

    val db: DatabaseHelper by injectLazy()

    val gson: Gson by injectLazy()

    val syncManager: LibrarySyncManager by injectLazy()

    val backupManager: BackupManager by lazy { BackupManager(db) }

    val networkService: NetworkHelper by injectLazy()

    val accountManager: AccountManager by lazy { AccountManager.get(context) }

    //TODO Exception handling
    override fun onPerformSync(account: Account?, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {
        //Read library snapshot
        var snapshot: LibrarySnapshot? = null
        try {
            if (syncManager.snapshotFile.exists()) {
                syncManager.snapshotFile.inputStream().bufferedReader().use {
                    snapshot = gson.fromJson(it, LibrarySnapshot::class.java)
                }
            }
        } catch(e: IOException) {
            Timber.e(e, "Failed to read last sync state from file!")
        }
        if(snapshot == null) {
            snapshot = LibrarySnapshot.empty()
        }
        val diff = DiffGenerator().generate(snapshot!!)
        val serialized = serializeLibraryDiff(diff).toString()
        Timber.d("SER: " + serialized)

        //Upload diff
        val api = TWApi.apiFromAccount(networkService, account!!)
        var token: String? = null
        //Three tries to authenticate
        for(i in 1 .. 3) {
            token = accountManager.blockingGetAuthToken(account,
                    SyncAccountAuthenticator.AUTH_TOKEN_TYPE,
                    true) ?: return
            //Verify we are authenticated first
            if (api.testAuthenticated(token)
                    .toBlocking()
                    .first()
                    .success) {
                break
            } else {
                //Unsuccessful, get a new auth token
                accountManager.invalidateAuthToken(SyncAccountAuthenticator.ACCOUNT_TYPE,
                        token)
                token = null
                Timber.w("Sync authentication token is invalid, retrieving a new one!")
            }
        }
        if(token == null) {
            return //Still no valid token, die
        }
        //Actually upload diff
        val result = api.syncDiff(token, serialized).toBlocking().first()
        Timber.d("RES: " + result)
        //TODO Process changes and write back to db

        //Write library snapshot to file
        val newSnapshot = LibrarySnapshot.fromDb(db)
        try {
            syncManager.snapshotFile.delete()
            syncManager.snapshotFile.outputStream().bufferedWriter().use {
                gson.toJson(newSnapshot, it)
            }
        } catch(e: IOException) {
            Timber.e(e, "Failed to save sync state to file!")
        }
    }

    fun serializeLibraryDiff(diff: LibraryDiff): JsonElement {
        /*val jObj = JsonObject()

        val modifiedCategories = JsonArray() + diff.modifiedCategories.map {
            backupManager.backupCategory(it)
        }
        val removedCategories = JsonArray() + diff.removedCategories

        val modifiedChapters = JsonArray() + diff.modifiedChapters.map { gson.toJsonTree(it) }
        val modifiedManga = JsonArray() + diff.modifiedManga.map { gson.toJsonTree(it) }

        val addedMangaCategories = JsonArray() + diff.addedMangaCategoryMappings.map { gson.toJsonTree(it) }
        val removedMangaCategories = JsonArray() + diff.removedMangaCategoryMappings.map { gson.toJsonTree(it) }

        jObj.put()*/
        return gson.toJsonTree(diff)
    }
}