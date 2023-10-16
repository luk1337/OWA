package com.luk.owa

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.luk.owa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val webView by lazy {
        binding.webview.apply {
            settings.javaScriptEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/200.0.0.0 Mobile Safari/537.36"

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
                        view?.evaluateJavascript(LOGIN_JS, null)
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

            forceDarkMode =
                sharedPreferences.getBoolean(SETTINGS_DARK_MODE, SETTINGS_DARK_MODE_DEFAULT)

            setDownloadListener { url: String?, _, contentDisposition: String?, mimeType: String?, _ ->
                getSystemService(DownloadManager::class.java).enqueue(
                    DownloadManager.Request(Uri.parse(url)).apply {
                        addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            @Suppress("DEPRECATION")
                            allowScanningByMediaScanner()
                        }

                        setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimeType)
                        )

                        setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                    }
                )
            }
        }
    }

    private var WebView.forceDarkMode: Boolean
        get() = when {
            WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) ->
                WebSettingsCompat.isAlgorithmicDarkeningAllowed(settings)
            WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) ->
                WebSettingsCompat.getForceDark(settings) == WebSettingsCompat.FORCE_DARK_ON
            else -> false
        }
        set(value) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, value)
                sharedPreferences.edit().putBoolean(SETTINGS_DARK_MODE, value).apply()
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    settings,
                    if (value) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
                sharedPreferences.edit().putBoolean(SETTINGS_DARK_MODE, value).apply()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        webView.loadUrl("${BuildConfig.OWA_HOST}/owa")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.add(Menu.NONE, 0, Menu.NONE, R.string.menu_refresh).apply {
            setIcon(R.drawable.outline_refresh_24)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener {
                CookieManager.getInstance().removeAllCookies {
                    webView.loadUrl("${BuildConfig.OWA_HOST}/owa")
                }
                true
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_dark_mode).apply {
                isCheckable = true
                isChecked = webView.forceDarkMode
                setOnMenuItemClickListener {
                    webView.forceDarkMode = !webView.forceDarkMode
                    isChecked = !isChecked
                    true
                }
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
            HTMLFormElement.prototype.submit.call(logonForm);
        """

        private const val SETTINGS_DARK_MODE = "SETTINGS_DARK_MODE"
        private const val SETTINGS_DARK_MODE_DEFAULT = false
    }
}