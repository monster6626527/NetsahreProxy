package com.admin.vpnshare.shizuku

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.admin.vpnshare.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class ShizukuTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = scope.launch {
            ShizukuTetherSessionService.liveState.collect { state ->
                updateTile(state)
            }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
        observeJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val current = ShizukuTetherSessionService.liveState.value
        when (current.status) {
            UiStatus.CONNECTED -> ShizukuTetherSessionService.stop(this)
            UiStatus.LOADING -> ShizukuTetherSessionService.stop(this)
            else -> {
                val gate = ShizukuGate.currentState()
                if (gate is ShizukuState.Ready) {
                    ShizukuTetherSessionService.start(this)
                } else {
                    ShizukuGate.remedy(this, gate)
                }
            }
        }
    }

    private fun updateTile(state: ShizukuSessionUiState) {
        val tile = qsTile ?: return
        when (state.status) {
            UiStatus.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Direct VPN Tether"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "${state.clientCount} clients"
                }
            }
            UiStatus.LOADING -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = "Connecting..."
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Starting"
                }
            }
            UiStatus.ERROR -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Direct Tether Error"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = state.lastError.take(20)
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Direct VPN Tether"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to start"
                }
            }
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_tethering)
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
