package com.countbot.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.countbot.app.ui.theme.CountBotTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "CountBot"
        private const val COUNTBOT_URL = "http://127.0.0.1:8000"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CountBotTheme {
                CountBotWebViewScreen()
            }
        }
    }

    @Composable
    fun CountBotWebViewScreen() {
        var webView by remember { mutableStateOf<WebView?>(null) }
        var progress by remember { mutableIntStateOf(0) }
        var isLoading by remember { mutableStateOf(true) }
        var hasError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var isNetworkAvailable by remember { mutableStateOf(true) }

        val context = LocalContext.current

        // Check network availability
        fun checkNetwork(): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }

        // Back handler for WebView history navigation
        BackHandler(enabled = webView != null) {
            val wv = webView ?: return@BackHandler
            if (wv.canGoBack()) {
                wv.goBack()
            } else {
                (context as? ComponentActivity)?.finish()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // WebView
                if (!hasError) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    // Enable media playback
                                    mediaPlaybackRequiresUserGesture = false
                                    // Performance optimizations
                                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                                }

                                // Force dark mode support
                                settings.setForceDark(WebSettings.FORCE_DARK_AUTO)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        hasError = false
                                        Log.d(TAG, "Page started loading: $url")
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url, favicon)
                                        isLoading = false
                                        progress = 100
                                        Log.d(TAG, "Page finished loading: $url")
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        // Only handle main frame errors
                                        if (request?.isForMainFrame == true) {
                                            val errorCode = error?.errorCode ?: -1
                                            val errorDesc = error?.description?.toString() ?: "Unknown error"
                                            Log.e(TAG, "WebView error: $errorCode - $errorDesc")

                                            hasError = true
                                            isLoading = false

                                            errorMessage = when (errorCode) {
                                                WebViewClient.ERROR_HOST_LOOKUP,
                                                WebViewClient.ERROR_CONNECT -> {
                                                    if (!checkNetwork()) {
                                                        context.getString(R.string.error_detail_network)
                                                    } else {
                                                        context.getString(R.string.error_detail_server)
                                                    }
                                                }
                                                WebViewClient.ERROR_TIMEOUT -> {
                                                    context.getString(R.string.error_detail_timeout)
                                                }
                                                else -> errorDesc
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        // Only allow loading from localhost
                                        if (url.startsWith("http://127.0.0.1") ||
                                            url.startsWith("http://localhost")
                                        ) {
                                            return false
                                        }
                                        Log.w(TAG, "Blocked external URL: $url")
                                        return true
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(
                                        view: WebView?,
                                        newProgress: Int
                                    ) {
                                        progress = newProgress
                                    }
                                }

                                webView = this
                                loadUrl(COUNTBOT_URL)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading progress bar at the top
                AnimatedVisibility(
                    visible = isLoading && !hasError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color(0xFF6200EE),
                        trackColor = Color(0xFFE0E0E0),
                    )
                }

                // Loading indicator (centered, shown initially)
                if (isLoading && !hasError && progress == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF6200EE),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = stringResource(R.string.loading),
                            modifier = Modifier.padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                    }
                }

                // Error page
                if (hasError) {
                    ErrorScreen(
                        errorMessage = errorMessage,
                        onRetry = {
                            hasError = false
                            isLoading = true
                            progress = 0
                            webView?.loadUrl(COUNTBOT_URL)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Composable
    fun ErrorScreen(
        errorMessage: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error icon using text symbol
            Text(
                text = "⚠",
                fontSize = 64.sp,
                color = Color(0xFF6200EE)
            )

            Text(
                text = stringResource(R.string.error_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = stringResource(R.string.error_message),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.retry_button),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
