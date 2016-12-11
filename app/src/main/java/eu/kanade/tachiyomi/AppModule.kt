package eu.kanade.tachiyomi

import android.app.Application
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {

            addSingletonFactory { PreferencesHelper(app) }

            addSingletonFactory { DatabaseHelper(app) }

            addSingletonFactory { ChapterCache(app) }

            addSingletonFactory { CoverCache(app) }

            addSingletonFactory { NetworkHelper(app) }

            addSingletonFactory { SourceManager(app) }

            addSingletonFactory { DownloadManager(app) }

            addSingletonFactory { MangaSyncManager(app) }

            addSingletonFactory { LibrarySyncManager(app) }

            addSingletonFactory { Gson() }

    }

}