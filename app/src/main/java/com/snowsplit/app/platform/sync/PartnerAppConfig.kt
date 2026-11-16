package com.snowsplit.app.platform.sync

import android.net.Uri

object PartnerAppConfig {
    const val OWN_PACKAGE = "com.snowsplit.app"
    const val PARTNER_PACKAGE = "com.snowdos.app"
    const val SYNC_PERMISSION = "com.snowapps.permission.THEME_SYNC"
    const val SYNC_ACTION = "com.snowapps.action.APPEARANCE_SYNC"
    const val PARTNER_AUTHORITY = "com.snowdos.app.syncprovider"
    const val OWN_AUTHORITY = "com.snowsplit.app.syncprovider"

    const val PATH_APPEARANCE = "appearance"
    const val PATH_BACKGROUND = "background"

    const val COLUMN_PAYLOAD_JSON = "payload_json"

    const val OWN_BACKGROUND_CONTENT_URI = "content://$OWN_AUTHORITY/$PATH_BACKGROUND"
    const val PARTNER_BACKGROUND_CONTENT_URI = "content://$PARTNER_AUTHORITY/$PATH_BACKGROUND"

    val PARTNER_APPEARANCE_URI: Uri =
        Uri.parse("content://$PARTNER_AUTHORITY/$PATH_APPEARANCE")
    val PARTNER_BACKGROUND_URI: Uri =
        Uri.parse(PARTNER_BACKGROUND_CONTENT_URI)

    const val EXTRA_PAYLOAD_JSON = "payload_json"
    const val EXTRA_SOURCE_PACKAGE = "source_package"
    const val EXTRA_SYNC_ID = "sync_id"
    const val EXTRA_UPDATED_AT = "updated_at_epoch_millis"
}
