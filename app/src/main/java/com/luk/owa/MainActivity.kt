package com.luk.owa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.luk.owa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val webView by lazy {
        binding.webview.apply {
            settings.javaScriptEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.0.0 Mobile Safari/537.36"

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)

                    binding.progressBar.progress = progress
                    binding.progressBar.isVisible = progress in 1..99
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    if (url?.startsWith("${BuildConfig.OWA_HOST}/lm_auth_proxy") == true ||
                        url?.startsWith("${BuildConfig.OWA_HOST}/owa/auth/logon.aspx") == true
                    ) {
                        view?.evaluateJavascript(LOGIN_JS) {}
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url?.startsWith(BuildConfig.OWA_HOST) == false) {
                        runCatching {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        return true
                    }
                    return super.shouldOverrideUrlLoading(view, url)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        webView.loadUrl(BuildConfig.OWA_HOST)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.add(Menu.NONE, 0, Menu.NONE, R.string.menu_refresh).apply {
            setIcon(android.R.drawable.ic_menu_rotate)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener {
                webView.loadUrl(BuildConfig.OWA_HOST)
                true
            }
        }
        return true
    }

    override fun onBackPressed() {
        repeat(2) {
            webView.evaluateJavascript(BACK_JS) {
                if (it == "true") {
                    val time = SystemClock.uptimeMillis()
                    webView.dispatchTouchEvent(
                        MotionEvent.obtain(
                            time, time, MotionEvent.ACTION_DOWN, BACK_ARROW_X, BACK_ARROW_Y, 0
                        )
                    )
                    webView.dispatchTouchEvent(
                        MotionEvent.obtain(
                            time, time, MotionEvent.ACTION_UP, BACK_ARROW_X, BACK_ARROW_Y, 0
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val BACK_ARROW_Y = 15.0f
        private const val BACK_ARROW_X = 15.0f

        private const val BACK_JS = """
            $("button[autoid=_ms_9] span[class='_fc_3 owaimg ms-Icon--arrowLeft ms-icon-font-size-21']")[0] !== undefined
        """
        private const val LOGIN_JS = """
            let logonForm = document.getElementsByName("logonForm")[0] || document.getElementById("logonForm");
            logonForm.username.value = "${BuildConfig.OWA_USERNAME}";
            logonForm.password.value = "${BuildConfig.OWA_PASSWORD}";
            HTMLFormElement.prototype.submit.call(logonForm)
        """
    }
}