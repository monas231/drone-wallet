package kr.or.kotsa.tsdronewallet.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kr.or.kotsa.tsdronewallet.R
import kr.or.kotsa.tsdronewallet.ui.main.MainFragment
import kr.or.kotsa.tsdronewallet.ui.main.MainViewModel

class MainActivity : AppCompatActivity() {

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE
                && id >= 0
                && id == mainViewModel.downloadId
            ) {
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = dm.query(DownloadManager.Query().setFilterById(id))

                if (!cursor.moveToFirst()) return

                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(columnIndex)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Toast.makeText(applicationContext, "다운로드가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
                else if (status == DownloadManager.STATUS_FAILED) {
                    Toast.makeText(applicationContext, "다운로드가 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        registerReceiver(downloadCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onDestroy() {
        unregisterReceiver(downloadCompleteReceiver)
        super.onDestroy()
    }

}