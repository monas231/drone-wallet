package kr.or.kotsa.tsdronewallet.ui.qrcode

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import kr.or.kotsa.tsdronewallet.R

class QrScannerActivity : AppCompatActivity() {

    companion object {
        const val QR_TEXT = "qr_text"
    }

    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        val scannerView = findViewById<CodeScannerView>(R.id.scannerView)

        codeScanner = CodeScanner(this, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.setDecodeCallback { result ->
            runOnUiThread {
                Intent().apply {
                    putExtra(QR_TEXT, result.text)
                }.also {
                    setResult(RESULT_OK, it)
                    finish()
                }
            }
        }
        codeScanner.setErrorCallback {
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(RESULT_CANCELED)
    }
}