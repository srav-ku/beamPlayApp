package app.cinephile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.cinephile.core.network.initServices
import app.cinephile.data.AndroidSessionStore
import app.cinephile.ui.App

class MainActivity : ComponentActivity() {
    // Locked portrait. The player overrides this to landscape while it is open,
    // and we force it back here on every resume so it can never get stuck rotating.
    override fun onResume() {
        super.onResume()
        if (!app.cinephile.ui.player.PlayerSession.isOpen) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Phase 1 wiring: the networking stack must exist before the first
        // composition so screens can reach `services` (BeamApi / TmdbApi /
        // VidaraApi) on first frame.
        initServices(isDebug = BuildConfig.DEBUG)

        // Beams the app context to common code so the local playback
        // memory (resume points / continue watching) can be read there.
        app.cinephile.ui.AppContextHolder.ctx = applicationContext

        setContent {
            App(AndroidSessionStore(applicationContext))
        }
    }
}
