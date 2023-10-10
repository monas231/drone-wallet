package kr.or.kotsa.tsdronewallet.ui.main

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class MyWebChromeClient(
    private val mContext: Context,
    private val onCloseListener: (() -> Unit)? = null
) : WebChromeClient() {

    private var mDialog: Dialog? = null
    private var mWebView: WebView? = null

    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
        super.onConsoleMessage(message, lineNumber, sourceID)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.d(MainFragment.TAG, consoleMessage.toString())
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // 퓁페이지에서 확인 팝업 호출
        AlertDialog.Builder(mContext)
            .setMessage(message)
            .setPositiveButton("확인") { dialog, which ->
                result?.confirm()
            }
            .setCancelable(false)
            .create()
            .show()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // 퓁페이지에서 확인, 취소 팝업 호출
        AlertDialog.Builder(mContext)
            .setMessage(message)
            .setPositiveButton("확인") { dialog, which ->
                result?.confirm()
            }
            .setNegativeButton("취소") { dialog, which ->
                result?.cancel()
            }
            .setCancelable(false)
            .create()
            .show()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        // 웹페이지에서 입력팝업 호출
        val editText = EditText(mContext)
        editText.setText(defaultValue)

        AlertDialog.Builder(mContext)
            .setView(editText)
            .setMessage(message)
            .setPositiveButton("확인") { dialog, which ->
                result?.confirm()
            }
            .setNegativeButton("취소") { dialog, which ->
                result?.cancel()
            }
            .setCancelable(false)
            .create()
            .show()
        return true
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        mWebView = getNewWebView {
            mDialog?.dismiss()
        }
        mDialog = Dialog(mContext).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(mWebView!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.insetsController?.hide(WindowInsets.Type.statusBars())
            }
            else {
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
            setOnKeyListener { dialog, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && (mWebViewClient == null || mWebViewClient?.disableBackKey == false)) {
                    if (mWebView?.canGoBack() == true) mWebView?.goBack()
                    else mDialog?.dismiss()

                    true
                }
                else false
            }
            setOnDismissListener {
                mWebView?.destroy()
                mWebView = null
                mDialog = null
            }
        }
        mDialog?.show()

        val transport = resultMsg?.obj
        if (transport != null && transport is WebViewTransport) {
            transport.webView = mWebView
            resultMsg.sendToTarget()
        }

        return true
    }

    override fun onCloseWindow(window: WebView?) {
//        window?.isVisible = false
        window?.destroy()
        onCloseListener?.invoke()
        super.onCloseWindow(window)
    }

    private var mWebViewClient: MyWebViewClient? = null

    private fun getNewWebView(listener: (() -> Unit)? = null) = WebView(mContext).apply {
        val mSettings = settings
        mSettings.javaScriptEnabled = true
        mSettings.domStorageEnabled = true
        mSettings.allowFileAccess = false
        mSettings.allowContentAccess = true
        mSettings.loadsImagesAutomatically = true
        mSettings.javaScriptCanOpenWindowsAutomatically = true
        mSettings.setSupportMultipleWindows(true)

        mWebViewClient = MyWebViewClient()
        mWebViewClient?.let {
            webViewClient = it
        }
        webChromeClient = MyWebChromeClient(mContext, listener)
    }
}