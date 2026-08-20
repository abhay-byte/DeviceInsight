// M2: Quick Settings tile cycling CALIPER media Paper → Carbon → Blueprint.
// Shares the single "caliper" DataStore medium key with SettingsViewModel,
// MainActivity and Glance widgets.
package com.ivarna.deviceinsight.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import com.ivarna.deviceinsight.ui.caliper.setMedium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MediaTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val current = mediumFlow.first()
            val next = when (current) {
                Medium.PAPER -> Medium.CARBON
                Medium.CARBON -> Medium.BLUEPRINT
                else -> Medium.PAPER
            }
            setMedium(next)
            syncTile()
        }
    }

    private fun syncTile() {
        scope.launch {
            val medium = mediumFlow.first() ?: Medium.PAPER
            val tile = qsTile ?: return@launch
            tile.label = medium.name
            tile.subtitle = getString(R.string.app_name)
            tile.contentDescription = "$medium drafting medium"
            tile.icon = androidx.core.graphics.drawable.IconCompat
                .createWithResource(this@MediaTileService, R.drawable.ic_tile_caliper)
                .toIcon(applicationContext)
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }
}