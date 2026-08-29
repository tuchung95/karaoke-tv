package com.athr.karaoketv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.athr.karaoketv.ui.KaraokeRoot
import com.athr.karaoketv.ui.KaraokeViewModel
import com.athr.karaoketv.ui.theme.KaraokeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        goFullScreen()

        // TV-BU / TV-BY: hold the screen awake only while a song is actually
        // playing. Holding it always would block the TV's Ambient Mode when the
        // app is just sitting on the menu.
        val player = (application as KaraokeApp).playerController
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                player.isPlaying.collect { playing ->
                    if (playing) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        setContent {
            KaraokeTheme {
                val vm: KaraokeViewModel = viewModel()
                KaraokeRoot(vm = vm, onExit = { finish() })
            }
        }
    }

    override fun onStop() {
        // TV-NP: a karaoke video singing on from the home screen, with no media
        // controls in the system UI to stop it, would be the worst of both worlds.
        (application as KaraokeApp).playerController.player.pause()
        super.onStop()
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
