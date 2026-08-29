package com.athr.karaoketv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.athr.karaoketv.ui.KaraokeRoot
import com.athr.karaoketv.ui.KaraokeViewModel
import com.athr.karaoketv.ui.theme.KaraokeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nobody wants the screen dimming halfway through a chorus.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goFullScreen()

        setContent {
            KaraokeTheme {
                val vm: KaraokeViewModel = viewModel()
                KaraokeRoot(vm = vm, onExit = { finish() })
            }
        }
    }

    override fun onDestroy() {
        // The player is application-scoped; only tear it down when the app is
        // really going away, not on a configuration change.
        if (isFinishing) {
            (application as KaraokeApp).playerController.release()
        }
        super.onDestroy()
    }

    private fun goFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
