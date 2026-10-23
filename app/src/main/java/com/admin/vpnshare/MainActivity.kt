package com.admin.vpnshare

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.admin.vpnshare.proxy.ProxyManager
import com.admin.vpnshare.ui.navigation.MainNavGraph
import com.admin.vpnshare.ui.theme.MyApplicationTheme
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make system status bar & navigation bar fully transparent (Edge-to-Edge like Telegram)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        
        // Request POST_NOTIFICATIONS permission for API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("vpn_share_prefs", MODE_PRIVATE) }
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            
            remember {
                LanguageManager.init(sharedPrefs)
                ThemeManager.init(sharedPrefs, isSystemDark = systemInDark)
                val savedUser = sharedPrefs.getString("proxy_user", "") ?: ""
                val savedPass = sharedPrefs.getString("proxy_pass", "") ?: ""
                ProxyManager.setCredentials(savedUser, savedPass)
                Unit
            }

            MyApplicationTheme {
                val viewModel: ProxyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                MainNavGraph(viewModel = viewModel)
            }
        }
    }
}
