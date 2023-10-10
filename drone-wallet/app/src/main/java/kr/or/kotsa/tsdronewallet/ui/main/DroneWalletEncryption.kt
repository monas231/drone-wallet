package kr.or.kotsa.tsdronewallet.ui.main

import android.content.Context
import android.graphics.Bitmap
import com.blankj.utilcode.util.EncodeUtils
import com.blankj.utilcode.util.EncryptUtils
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import okio.use
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DroneWalletEncryption {
    private const val KEY_SOURCE = "greenbros"
    private const val TRANSFORMATION = "AES/CTR/NoPadding"
    private val KEY = EncryptUtils.encryptSHA256(KEY_SOURCE.toByteArray(Charsets.UTF_8))
    private val IV = byteArrayOf(1, 5, 7, 7, 0, 9, 9, 0, 3, 9, 6, 6, 0, 6, 1, 7)

    /**
     * 주어진 데이터를 암호화된 qr text 로 변환한다
     */
    fun getEncryptedQrText(data: String): String? {
        val encryptedData = EncryptUtils.encryptAES2Base64(
            data.toByteArray(Charsets.UTF_8),
            KEY,
            TRANSFORMATION,
            IV
        )
        val encodedBitmap = BarcodeEncoder().encodeBitmap(
            encryptedData.toString(Charsets.UTF_8),
            BarcodeFormat.QR_CODE,
            400,
            400
        )
        val baos = ByteArrayOutputStream()
        val isSuccess = encodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)

        return if (isSuccess) EncodeUtils.base64Encode2String(baos.toByteArray()) else null
    }

    /**
     * 암호화된 text 를 데이터로 변환한다
     */
    fun getDecryptedQrText(data: String) =
        EncryptUtils.decryptBase64AES(data.toByteArray(Charsets.UTF_8), KEY, TRANSFORMATION, IV)
            .toString(Charsets.UTF_8)

    fun saveEncryptedImage(context: Context, vcId: String, type: String, imageData: String) {
        // vcId 암호화
        val encryptVcId = EncryptUtils.encryptAES2Base64(
            vcId.toByteArray(Charsets.UTF_8),
            KEY,
            TRANSFORMATION,
            IV
        )
        // image data 를 암호화된 파일로 생성한다.
        val secretKey = SecretKeySpec(KEY, "AES")
        val params = IvParameterSpec(IV)
        val aes = Cipher.getInstance(TRANSFORMATION)
        aes.init(Cipher.ENCRYPT_MODE, secretKey, params)
        // 암호화된 vcId 에 type 을 붙인다
        val fileName = "${String(encryptVcId)}_$type"
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
            with(CipherOutputStream(fos, aes)) {
                write(imageData.toByteArray(Charsets.UTF_8))
                flush()
                close()
            }
        }
    }

    fun loadEncryptedImage(context: Context, vcId: String, type: String): String {
        // vcId 암호화
        val encryptVcId = EncryptUtils.encryptAES2Base64(
            vcId.toByteArray(Charsets.UTF_8),
            KEY,
            TRANSFORMATION,
            IV
        )
        val secretKey = SecretKeySpec(KEY, "AES")
        val params = IvParameterSpec(IV)
        val aes = Cipher.getInstance(TRANSFORMATION)
        aes.init(Cipher.DECRYPT_MODE, secretKey, params)
        // 암호화된 vcId 에 type 을 붙인다
        val fileName = "${String(encryptVcId)}_$type"
        context.openFileInput(fileName).use { fis ->
            with(CipherInputStream(fis, aes)) {
                return readBytes().toString(Charsets.UTF_8)
            }
        }
    }

}