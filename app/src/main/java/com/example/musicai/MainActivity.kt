package com.example.musicai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface // 確保引入這個
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
private const val TOOLBAR_AUTO_HIDE_DELAY_MS = 1500L // 修改為 3 秒
private const val JAVASCRIPT_INTERFACE_NAME = "AndroidInterface"

// JavaScript Interface 類別
class WebViewJavaScriptInterface(private val onShowToolbar: () -> Unit) {
    // 確保 JS 調用的方法在主線程執行以更新 UI
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
    private var showToolbarState by mutableStateOf(false) // 將 showToolbar 提升為 Activity 的狀態

    // JavaScript Interface 實例
    private lateinit var jsInterface: WebViewJavaScriptInterface

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
                    onBackPressedDispatcher.onBackPressed() // 使用 Activity 的 onBackPressedDispatcher
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContent {
            MusicAiTheme {
                // 使用 showToolbarState 來驅動 UI
                var currentShowToolbar by remember { mutableStateOf(showToolbarState) }
                val context = LocalContext.current

                // 監聽 showToolbarState 的變化來更新 currentShowToolbar
                // 這樣可以確保 LaunchedEffect 正確地基於 Compose 可觀察的狀態觸發
                LaunchedEffect(showToolbarState) {
                    currentShowToolbar = showToolbarState
                }

                LaunchedEffect(currentShowToolbar) {
                    if (currentShowToolbar) {
                        delay(TOOLBAR_AUTO_HIDE_DELAY_MS)
                        // 再次檢查，避免在用戶連續觸發時，舊的計時器錯誤地關閉了新的顯示請求
                        if (currentShowToolbar && showToolbarState) {
                            showToolbarState = false
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    delay(500) // 稍微延遲初始的顯示和隱藏，給 WebView 一點載入時間
                    showToolbarState = true
                    delay(TOOLBAR_AUTO_HIDE_DELAY_MS - 1000L)
                    if (showToolbarState) showToolbarState = false
                }

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
                        javascriptInterface = jsInterface, // 傳遞 JS Interface
                        onWebViewReady = { webView ->
                            webViewInstance = webView
                        }
                    )

                    FloatingIslandToolbar(
                        isVisible = currentShowToolbar, // 使用 currentShowToolbar
                        onShare = {
                            val currentUrl = webViewInstance?.url
                            if (!currentUrl.isNullOrEmpty()) {
                                shareUrl(context, currentUrl)
                            }
                            showToolbarState = false
                        },
                        onGoHomeWithRefreshIcon = {
                            webViewInstance?.loadUrl(HOME_URL)
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
    javascriptInterface: WebViewJavaScriptInterface, // 接收 JS Interface
    onWebViewReady: (WebView) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT) // 使用 android.graphics.Color
                val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isSystemInDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                android.util.Log.d("WebViewTheme", "System is in dark mode: $isSystemInDarkMode") // 新增 Log
                // .

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // API 33+
                    settings.isAlgorithmicDarkeningAllowed = isSystemInDarkMode // 如果系統是深色，設為 true
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) { // API 29-32
                    if (isSystemInDarkMode) {
                        settings.forceDark = android.webkit.WebSettings.FORCE_DARK_AUTO // 確保是 AUTO
                    } else {
                        settings.forceDark = android.webkit.WebSettings.FORCE_DARK_OFF
                    }
                }



                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // 將 JS Interface 添加到 WebView
                addJavascriptInterface(javascriptInterface, JAVASCRIPT_INTERFACE_NAME)

                webViewClient = object : CustomWebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 頁面載入完成後注入 JavaScript
                        val script = """
                            (function() {
                                let debounceTimeout;
                                function checkScroll() {
                                    clearTimeout(debounceTimeout);
                                    debounceTimeout = setTimeout(function() {
                                        var atTop = window.scrollY <= 0;
                                        var atBottom = (window.innerHeight + window.scrollY) >= document.documentElement.scrollHeight - 5; // 5px 容差
                                        
                                        if (atTop || atBottom) {
                                            if (typeof ${JAVASCRIPT_INTERFACE_NAME} !== 'undefined' && ${JAVASCRIPT_INTERFACE_NAME}.showToolbarFromScroll) {
                                                ${JAVASCRIPT_INTERFACE_NAME}.showToolbarFromScroll();
                                            }
                                        }
                                    }, 150); // 150ms 防抖
                                }
                                window.addEventListener('scroll', checkScroll, false);
                                // 初始檢查一次，以防頁面載入時就已在頂部或底部
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
        update = { webView -> /* 更新邏輯 */ },
        modifier = modifier
    )
}

// CustomWebViewClient 保持不變或按需修改
open class CustomWebViewClient : WebViewClient() { // 改為 open 以便 MyWebView 中繼承和覆寫
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        return if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            false
        } else {
            super.shouldOverrideUrlLoading(view, request)
        }
    }
}