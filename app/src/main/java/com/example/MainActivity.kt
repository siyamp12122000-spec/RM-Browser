package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BrowserMainScreen
import com.example.ui.theme.RMBrowserTheme
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.BrowserViewModelFactory

class MainActivity : ComponentActivity() {

    private val app by lazy { application as RMBrowserApp }

    private val viewModel: BrowserViewModel by viewModels {
        BrowserViewModelFactory(
            database = app.database,
            kahafGuardEngine = app.kahafGuardEngine,
            kahafDnsResolver = app.kahafDnsResolver,
            downloadManager = app.downloadManager
        )
    }

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intentData = result.data
            val uris = when {
                intentData?.clipData != null -> {
                    val count = intentData.clipData!!.itemCount
                    Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
                }
                intentData?.data != null -> {
                    arrayOf(intentData.data!!)
                }
                else -> null
            }
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle external URL intent
        handleIntentUrl(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            RMBrowserTheme(themeMode = settings.themeMode) {
                BrowserMainScreen(
                    viewModel = viewModel,
                    onShowFileChooser = { callback, params ->
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = callback
                        val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        try {
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (_: Exception) {
                            fileUploadCallback = null
                            false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentUrl(intent)
    }

    private fun handleIntentUrl(intent: Intent?) {
        val action = intent?.action
        val data: Uri? = intent?.data
        if (Intent.ACTION_VIEW == action && data != null) {
            val url = data.toString()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                viewModel.openNewTab(url)
            }
        }
    }
}
