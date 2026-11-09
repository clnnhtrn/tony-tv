package app.tony

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import app.tony.player.LocalPlayers
import app.tony.player.Players
import app.tony.player.Stream
import app.tony.ui.DetailsScreen
import app.tony.ui.FavouritesScreen
import app.tony.ui.HomeScreen
import app.tony.ui.SearchScreen
import app.tony.ui.SettingsScreen
import app.tony.ui.T
import app.tony.ui.ToastHost
import app.tony.ui.TonyTheme
import app.tony.ui.WatchScreen
import app.tony.ui.WelcomeScreen
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okio.Path.Companion.toOkioPath

class TonyApp : Application(), SingletonImageLoader.Factory {
    // Small caches: the Chromecast has little RAM and ~8 GB storage (CLAUDE.md 9).
    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory()) }
        .apply { if (BuildConfig.DEBUG) logger(coil3.util.DebugLogger()) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images").toOkioPath()).maxSizeBytes(120L * 1024 * 1024).build() }
        .build()
}

class MainActivity : ComponentActivity() {
    private val state: AppState by viewModels()
    private var players: Players? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val p = remember { players ?: Players(Stream(this, preview = false), Stream(this, preview = true)).also { players = it } }
            TonyTheme {
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                CompositionLocalProvider(LocalPlayers provides p, androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides app.tony.ui.MinimalScroll) {
                    Box(Modifier.fillMaxSize().background(T.Bg)) {
                        // Proper loading screen while Tony starts and fetches the list (min ~1.2 s, so it never flashes)
                        var minShown by remember { mutableStateOf(false) }
                        androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(1200); minShown = true }
                        val booting = !state.ready || !minShown || (state.screen != Screen.Welcome && state.loading && state.countryRows.isEmpty() && !state.offline)
                        androidx.compose.animation.AnimatedVisibility(booting, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(500))) { app.tony.ui.LoadingScreen() }
                        if (state.ready && !booting) {
                            when (val sc = state.screen) {
                                Screen.Welcome -> WelcomeScreen(state)
                                Screen.Home -> HomeScreen(state)
                                Screen.Movies -> app.tony.ui.MoviesScreen(state)
                                is Screen.Category -> app.tony.ui.CategoryScreen(state, sc.name)
                                Screen.Favourites -> FavouritesScreen(state)
                                Screen.Search -> SearchScreen(state)
                                Screen.Settings -> SettingsScreen(state)
                                is Screen.Details -> DetailsScreen(state, sc.channel)
                                is Screen.Watch -> WatchScreen(state, sc.channel, sc.list)
                            }
                        }
                        ToastHost(state.toast)
                    }
                }
            }
        }
    }

    /**
     * Back always means "go back one step" (close a panel, or the previous screen).
     * Compose on TV otherwise spends the first Back moving focus out of the current group,
     * so every overlay needed two presses.
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled && !state.handleBack()) finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    // Safety (CLAUDE.md 5a): nothing plays or runs when Tony isn't on screen.
    override fun onStart() {
        super.onStart()
        state.onForeground()
    }

    override fun onStop() {
        super.onStop()
        state.foreground = false
        players?.main?.stop(); players?.preview?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) { players?.main?.release(); players?.preview?.release(); players = null }
    }
}
