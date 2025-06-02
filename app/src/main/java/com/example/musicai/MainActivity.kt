package com.example.musicai

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import android.net.Uri
import com.example.musicai.ui.theme.MusicAiTheme

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicAiTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),

                    bottomBar = {
                        BottomAppBar {
                            IconButton(onClick = {
                                webView?.reload()
                            }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "重新載入")
                            }
                            IconButton(onClick = {
                                webView?.url?.let { url ->
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }
                                    startActivity(Intent.createChooser(intent, "分享此頁面"))
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "分享")
                            }
                        }
                    }
                ) { innerPadding ->
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadsImagesAutomatically = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                loadUrl("https://www.google.com")  // ← 建議加上 www.
                                webView = this
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicAiTheme {
        Greeting("Android")
    }
}