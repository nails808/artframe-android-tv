package cc.wallhaven.artframe

import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import okio.Path.Companion.toOkioPath
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder().maxSizePercent(this@MainActivity, 0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "wallhaven_images").toOkioPath())
                    .maxSizeBytes(2L * 1024L * 1024L * 1024L)
                    .build()
            }
            .build()
        setContent { ArtframeApp(imageLoader) }
    }
}

@Composable
private fun ArtframeApp(imageLoader: ImageLoader) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val client = remember { WallhavenClient() }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf(store.query) }
    var apiKey by remember { mutableStateOf(store.apiKey) }
    var duration by remember { mutableIntStateOf(store.durationSeconds) }
    var sfw by remember { mutableStateOf(store.includeSfw) }
    var sketchy by remember { mutableStateOf(store.includeSketchy) }
    var nsfw by remember { mutableStateOf(store.includeNsfw) }
    var wallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val current = wallpapers.getOrNull(index)

    fun persist() {
        store.apiKey = apiKey
        store.query = query
        store.durationSeconds = duration
        store.includeSfw = sfw
        store.includeSketchy = sketchy
        store.includeNsfw = nsfw
    }

    fun search() {
        persist()
        loading = true
        error = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    client.search(
                        query, apiKey, sfw, sketchy, nsfw,
                        targetWidth = maxOf(1920, windowManager.defaultDisplay.mode.physicalWidth),
                        targetHeight = maxOf(1080, windowManager.defaultDisplay.mode.physicalHeight)
                    )
                }
                wallpapers = result.wallpapers
                index = 0
                showSettings = false
                if (result.wallpapers.isEmpty()) {
                    error = "No matching 16:9 wallpapers at 1920×1080 or higher. Try another term or broaden your search."
                    showSettings = true
                }
            } catch (t: Throwable) {
                error = t.message ?: "Unable to search Wallhaven."
                showSettings = true
            } finally {
                loading = false
            }
        }
    }

    // Preload the next original image while the current one is on screen.
    LaunchedEffect(index, wallpapers) {
        val next = wallpapers.getOrNull(index + 1)
        if (next != null) {
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(next.path)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    LaunchedEffect(index, wallpapers, showSettings) {
        if (!showSettings && wallpapers.isNotEmpty()) {
            delay(duration * 1000L)
            index = (index + 1) % wallpapers.size
        }
    }

    BackHandler(enabled = !showSettings) {
        showInfo = false
        showSettings = true
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF171717),
            primary = Color(0xFFE8E8E8),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && !showSettings) {
                        when (event.nativeKeyEvent.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                index = (index - 1 + wallpapers.size) % wallpapers.size
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                index = (index + 1) % wallpapers.size
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> {
                                showInfo = !showInfo
                                true
                            }
                            AndroidKeyEvent.KEYCODE_MENU,
                            AndroidKeyEvent.KEYCODE_SETTINGS -> {
                                showSettings = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .focusable()
        ) {
            if (showSettings) {
                SettingsScreen(
                    query = query, onQuery = { query = it },
                    apiKey = apiKey, onApiKey = { apiKey = it },
                    duration = duration, onDuration = { duration = it.coerceIn(10, 3600) },
                    sfw = sfw, onSfw = { sfw = it },
                    sketchy = sketchy, onSketchy = { sketchy = it },
                    nsfw = nsfw, onNsfw = { nsfw = it },
                    loading = loading, error = error,
                    onStart = { search() }
                )
            } else if (current != null) {
                AnimatedContent(
                    targetState = current.path,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(1200)) togetherWith
                            fadeOut(animationSpec = tween(1200))
                    },
                    label = "wallpaperCrossfade"
                ) { imagePath ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imagePath)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = "Wallhaven wallpaper ${current.id}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                if (showInfo) {
                    Column(
                        Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color(0x99000000))
                            .padding(horizontal = 32.dp, vertical = 20.dp)
                    ) {
                        Text("WALLHAVEN  •  ${current.id}", fontSize = 13.sp, color = Color.LightGray)
                        Text("${current.width} × ${current.height}  •  ${index + 1} / ${wallpapers.size}", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Text("ARTFRAME", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    query: String, onQuery: (String) -> Unit,
    apiKey: String, onApiKey: (String) -> Unit,
    duration: Int, onDuration: (Int) -> Unit,
    sfw: Boolean, onSfw: (Boolean) -> Unit,
    sketchy: Boolean, onSketchy: (Boolean) -> Unit,
    nsfw: Boolean, onNsfw: (Boolean) -> Unit,
    loading: Boolean, error: String?, onStart: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF090909)).padding(horizontal = 64.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("ARTFRAME", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("WALLHAVEN • DIGITAL ART DISPLAY", fontSize = 12.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SEARCH TERM", color = Color.LightGray, fontSize = 12.sp)
                InputField(query, onQuery, "e.g. dark abstract")
                Text("WALLHAVEN API KEY", color = Color.LightGray, fontSize = 12.sp)
                InputField(apiKey, onApiKey, "Paste your API key")
                Text("Generate a key at wallhaven.cc/settings/account", color = Color.Gray, fontSize = 11.sp)
                Text("CONTENT CATEGORIES", color = Color.LightGray, fontSize = 12.sp)
                CategoryToggle("SFW", sfw, onSfw)
                CategoryToggle("Sketchy", sketchy, onSketchy)
                CategoryToggle("NSFW  •  Wallhaven account/API key required", nsfw, onNsfw)
            }
            Column(Modifier.weight(0.75f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DISPLAY", color = Color.LightGray, fontSize = 12.sp)
                Text("Aspect ratio", color = Color.Gray)
                Text("16:9  •  Fixed", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Minimum image size", color = Color.Gray)
                Text("1920 × 1080", color = Color.White, fontSize = 20.sp)
                Text("Preferred display", color = Color.Gray)
                Text("3840 × 2160 (4K)", color = Color.White, fontSize = 20.sp)
                Text("SECONDS PER IMAGE", color = Color.LightGray, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onDuration((duration - 10).coerceAtLeast(10)) }) { Text("−") }
                    Text("$duration sec", color = Color.White, fontSize = 20.sp)
                    Button(onClick = { onDuration((duration + 10).coerceAtMost(3600)) }) { Text("+") }
                }
                Text("Use left/right on the remote to skip. OK shows image details. Back opens settings.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        if (error != null) Text(error, color = Color(0xFFFF8A80), fontSize = 13.sp)
        Button(
            onClick = onStart,
            enabled = !loading && query.isNotBlank() && (sfw || sketchy || nsfw),
            modifier = Modifier.width(240.dp).height(52.dp)
        ) {
            Text(if (loading) "SEARCHING…" else "START DISPLAY", fontSize = 15.sp)
        }
    }
}

@Composable
private fun InputField(value: String, onValue: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF242424), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = Color.Gray, fontSize = 16.sp)
                inner()
            }
        }
    )
}

@Composable
private fun CategoryToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}
