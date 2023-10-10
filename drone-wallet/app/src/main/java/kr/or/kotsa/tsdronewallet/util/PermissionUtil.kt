package kr.or.kotsa.tsdronewallet.util

import android.Manifest
import android.os.Build

val startPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf()
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
} else {
    arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}

val cameraPermissions = arrayOf(
    Manifest.permission.CAMERA
)