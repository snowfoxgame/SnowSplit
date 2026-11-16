package com.snowsplit.app

import android.app.Application
import com.snowsplit.app.core.common.AppBitmapCache
import com.snowsplit.app.platform.sync.PartnerAppDetector
import com.snowsplit.app.platform.sync.ThemeSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SnowSplitApplication : Application() {

    @Inject
    lateinit var partnerAppDetector: PartnerAppDetector

    @Inject
    lateinit var themeSyncManager: ThemeSyncManager

    override fun onCreate() {
        super.onCreate()
        partnerAppDetector.refreshAsync()
        themeSyncManager.initialize()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        @Suppress("DEPRECATION")
        when {
            level >= TRIM_MEMORY_UI_HIDDEN -> AppBitmapCache.evictAll()
            level >= TRIM_MEMORY_RUNNING_LOW -> AppBitmapCache.trimToHalf()
        }
    }
}
