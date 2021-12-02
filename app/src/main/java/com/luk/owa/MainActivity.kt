package com.luk.owa

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
                    binding.progressBar.isVisible = progress < 100
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
                webView.reload()
                true
            }
        }
        return true
    }

    override fun onBackPressed() {}

    companion object {
        private const val LOGIN_JS = """
            let logonForm = document.getElementsByName("logonForm")[0] || document.getElementById("logonForm");
            logonForm.username.value = "${BuildConfig.OWA_USERNAME}";
            logonForm.password.value = "${BuildConfig.OWA_PASSWORD}";
            HTMLFormElement.prototype.submit.call(logonForm)
        """
    }
}