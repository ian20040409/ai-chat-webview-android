package com.example.musicai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface // 確保引入這個
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox // Added for Pull-to-refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.musicai.ui.theme.MusicAiTheme
import kotlinx.coroutines.delay

const val HOME_URL = "https://lnu.nttu.edu.tw/"
private const val TOOLBAR_AUTO_HIDE_DELAY_MS = 1500L
private const val JAVASCRIPT_INTERFACE_NAME = "AndroidInterface"

// JavaScript Interface 類別
class WebViewJavaScriptInterface(private val onShowToolbar: () -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun showToolbarFromScroll() {
        mainHandler.post {
            onShowToolbar()
        }
    }
}

class MainActivity : ComponentActivity() {
    private var webViewInstance: WebView? = null
    private var showToolbarState by mutableStateOf(false)
    private lateinit var jsInterface: WebViewJavaScriptInterface
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jsInterface = WebViewJavaScriptInterface {
            showToolbarState = true
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webViewInstance?.canGoBack() == true) {
                    webViewInstance?.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContent {
            MusicAiTheme {
                var currentShowToolbar by remember { mutableStateOf(showToolbarState) }
                val context = LocalContext.current
                var isWebViewRefreshing by remember { mutableStateOf(false) } // State for pull-to-refresh

                LaunchedEffect(showToolbarState) {
                    currentShowToolbar = showToolbarState
                }

                LaunchedEffect(currentShowToolbar) {
                    if (currentShowToolbar) {
                        delay(TOOLBAR_AUTO_HIDE_DELAY_MS)
                        if (currentShowToolbar && showToolbarState) {
                            showToolbarState = false
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    delay(500)
                    showToolbarState = true
                    delay(TOOLBAR_AUTO_HIDE_DELAY_MS - 1000L)
                    if (showToolbarState) showToolbarState = false
                }

                // PullToRefreshBox wraps the content that needs pull-to-refresh
                PullToRefreshBox(
                    isRefreshing = isWebViewRefreshing,
                    onRefresh = {
                        isWebViewRefreshing = true
                        webViewInstance?.reload()
                        // isWebViewRefreshing will be set to false in onPageReloaded callback
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showToolbarState = true }
                                )
                            }
                    ) {
                        MyWebView(
                            modifier = Modifier.fillMaxSize(),
                            initialUrl = HOME_URL,
                            javascriptInterface = jsInterface,
                            onWebViewReady = { webView ->
                                webViewInstance = webView
                                webView.setOnTouchListener { _, event ->
                                    if (event.action == MotionEvent.ACTION_UP) {
                                        showToolbarState = true
                                    }
                                    false
                                }
                            },
                            onPageReloaded = { // Callback to stop the refresh indicator
                                isWebViewRefreshing = false
                            }
                        )

                        FloatingIslandToolbar(
                            isVisible = currentShowToolbar,
                            onShare = {
                                val currentUrl = webViewInstance?.url
                                if (!currentUrl.isNullOrEmpty()) {
                                    shareUrl(context, currentUrl)
                                }
                                showToolbarState = false
                            },
                            onGoHomeWithRefreshIcon = {
                                webViewInstance?.loadUrl(HOME_URL)
                                // Optionally, show refresh indicator while loading home,
                                // then onPageReloaded will hide it.
                                // isWebViewRefreshing = true
                                showToolbarState = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingIslandToolbar(
    isVisible: Boolean,
    onShare: () -> Unit,
    onGoHomeWithRefreshIcon: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(animationSpec = tween(300)),
        modifier = modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            IconButton(onClick = onGoHomeWithRefreshIcon) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "回到首頁",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "分享",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun shareUrl(context: Context, url: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MyWebView(
    modifier: Modifier = Modifier,
    initialUrl: String,
    javascriptInterface: WebViewJavaScriptInterface,
    onWebViewReady: (WebView) -> Unit,
    onPageReloaded: () -> Unit // New callback for when page reload finishes
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isSystemInDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                android.util.Log.d("WebViewTheme", "System is in dark mode: $isSystemInDarkMode")

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = isSystemInDarkMode
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    settings.forceDark = if (isSystemInDarkMode) android.webkit.WebSettings.FORCE_DARK_AUTO else android.webkit.WebSettings.FORCE_DARK_OFF
                }

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addJavascriptInterface(javascriptInterface, JAVASCRIPT_INTERFACE_NAME)

                // Pass the onPageReloaded callback to CustomWebViewClient
                webViewClient = object : CustomWebViewClient(onPageReloaded) {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url) // This will call onPageReloaded
                        val script = """
                            (function() {
                                let debounceTimeout;
                                function checkScroll() {
                                    clearTimeout(debounceTimeout);
                                    debounceTimeout = setTimeout(function() {
                                        var atTop = window.scrollY <= 0;
                                        var atBottom = (window.innerHeight + window.scrollY) >= document.documentElement.scrollHeight - 5;
                                        
                                        if (atTop || atBottom) {
                                            if (typeof ${JAVASCRIPT_INTERFACE_NAME} !== 'undefined' && ${JAVASCRIPT_INTERFACE_NAME}.showToolbarFromScroll) {
                                                ${JAVASCRIPT_INTERFACE_NAME}.showToolbarFromScroll();
                                            }
                                        }
                                    }, 150);
                                }
                                window.addEventListener('scroll', checkScroll, false);
                                checkScroll();
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                val zoomDisableScript = """
                    var meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                    document.head.appendChild(meta);
                """.trimIndent()
                evaluateJavascript("javascript:(function() { $zoomDisableScript })()", null)
                settings.userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

                loadUrl(initialUrl)
                onWebViewReady(this)
            }
        },
        update = { /* webView -> Updates if needed */ },
        modifier = modifier
    )
}

// Modified CustomWebViewClient to accept and call onPageReloaded
open class CustomWebViewClient(private val onPageReloaded: () -> Unit) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        return if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            false
        } else {
            // Handle other schemes or let the system handle them
            // For example, you might want to launch an intent for "tel:", "mailto:", etc.
            // For simplicity here, we just call super.
            super.shouldOverrideUrlLoading(view, request)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageReloaded() // Call the callback when any page load finishes
    }
}