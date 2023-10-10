package kr.or.kotsa.tsdronewallet.ui.main

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.TimeUtils
import com.google.gson.Gson
import com.markany.did_sdk.viewmodel.DidViewModel
import com.permissionx.guolindev.PermissionMediator
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.or.kotsa.tsdronewallet.BuildConfig
import kr.or.kotsa.tsdronewallet.R
import kr.or.kotsa.tsdronewallet.constant.Constants
import kr.or.kotsa.tsdronewallet.constant.DidModel
import kr.or.kotsa.tsdronewallet.constant.WebDataCommand
import kr.or.kotsa.tsdronewallet.constant.WebDataResult
import kr.or.kotsa.tsdronewallet.databinding.FragmentMainBinding
import kr.or.kotsa.tsdronewallet.model.CredentialSubject
import kr.or.kotsa.tsdronewallet.model.DroneException
import kr.or.kotsa.tsdronewallet.ui.qrcode.QrScannerActivity
import kr.or.kotsa.tsdronewallet.util.*
import org.json.JSONObject
import java.util.concurrent.Executor
import kotlin.math.abs

class MainFragment : Fragment() {

    companion object {
        const val QR_DATE_TYPE = "yyyyMMddHHmmss"
        const val QR_LIMIT_TIME = 1000 * 60 * 3
        val TAG: String = this::class.java.simpleName
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: FragmentMainBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var mSdk: DidViewModel
    private val prefUtil by lazy { PreferenceUtil(requireContext()) }
    private var masterKey: Array<String>? = null
    private lateinit var permissionX: PermissionMediator
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val getBiometricResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    }
    // qr 코드 스캔 요청
    private val getQrScanResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val qrText = it.data?.getStringExtra(QrScannerActivity.QR_TEXT)
        val sendData = try {
            if (it.resultCode == Activity.RESULT_OK && qrText != null) {
                // 성공이면 복호화된 데이터를 검증요청한다
                val decryptedText = try {
                    DroneWalletEncryption.getDecryptedQrText(qrText)
                } catch (e: Exception) {
                    throw DroneException("not acceptable")
                }
                val jsonData = JSONObject(decryptedText)
                val date = try {
                    jsonData.getString("date")
                } catch (e: Exception) {
                    throw DroneException("not found date")
                }
                val url = try {
                    jsonData.getString("url")
                } catch (e: Exception) {
                    throw DroneException("not found url")
                }
                val didId = try {
                    jsonData.getString("didId")
                } catch (e: Exception) {
                    throw DroneException("not found didId")
                }

                val millis = TimeUtils.getSafeDateFormat(QR_DATE_TYPE).parse(date)?.time ?: 0L
                val isAllowed = (System.currentTimeMillis() - millis) < QR_LIMIT_TIME
                if (isAllowed) {
                    webDataResponse.getScanQrcodeVerifyResult(url, didId)
                } else {
                    throw DroneException("time over")
                }
            } else {
                throw DroneException("cancel")
            }
        } catch (e: Exception) {
            webDataResponse.createWebParamData(
                WebDataCommand.SCAN_QRCODE_VERIFY,
                WebDataResult.REQUEST_FAIL,
                e.message
            )
        }

        if (BuildConfig.DEBUG) {
            Toast.makeText(requireContext(), "send to web: $sendData", Toast.LENGTH_SHORT).show()
        }
        binding.wvMain.loadUrl("javascript:appToWeb('$sendData');")
    }
    private val mGson by lazy { Gson() }
    private var backPressedTime = 0L
    private val ioDispatcher by lazy { Dispatchers.IO }
    private val calcDispatcher by lazy { Dispatchers.Default }
    private val mainDispatcher by lazy { Dispatchers.Main }
    private lateinit var biometricManager: BiometricManager
    private lateinit var webDataResponse: WebDataResponse
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mWebViewClient.disableBackKey) {
                finishApp()
                return
            }

            if (binding.wvMain.canGoBack()) {
                binding.wvMain.goBack()
            }
            else {
                finishApp()
            }
        }

    }
    private lateinit var mWebViewClient: MyWebViewClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initVariables()
        initUI()
        checkStoragePermissions {}
        // 초기화
        if (masterKey == null) {
            setGateway()
            setMasterKey()
        }
    }

    private fun initVariables() {
        permissionX = PermissionX.init(this)
        // sdk, viewmodel 초기화
        mSdk = DidViewModel.get(requireActivity(), true)
        webDataResponse = WebDataResponse(mSdk)
        masterKey = prefUtil.getStringSet(PreferenceKey.MASTER_KEY)?.toTypedArray()
        biometricManager = BiometricManager.from(requireContext())
    }

    private fun initUI() {
        binding.wvMain.addJavascriptInterface(
            WebAppInterface(),
            "moren"
        )
        mWebViewClient = MyWebViewClient()
        binding.wvMain.webViewClient = mWebViewClient
        binding.wvMain.webChromeClient = MyWebChromeClient(mContext = requireContext())
        binding.wvMain.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            checkStoragePermissions { isAllowed ->
                if (isAllowed) {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    val cookies = CookieManager.getInstance().getCookie(url)
                    DownloadManager.Request(Uri.parse(url)).apply {
                        addRequestHeader("Cookie", cookies)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription("파일 다운로드 중...")
                        setTitle(fileName)
                        setAllowedOverMetered(true)
                        setAllowedOverRoaming(true)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }.let {
                        val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val id = dm.enqueue(it)
                        viewModel.downloadId = id
                    }

                }
            }
        }
        with(binding.wvMain.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
//        binding.wvMain.loadUrl("http://si.ibleaders.co.kr:18088/html_dev/splash.html")
//        binding.wvMain.loadUrl("http://dron.wellpage.co.kr/")
        binding.wvMain.loadUrl(BuildConfig.LOAD_URI)
//        binding.wvMain.loadUrl("https://www.kotsa.or.kr/dcert/html_dev/splash.html")
//        binding.wvMain.loadUrl("http://dron.wellpage.co.kr/html_dev/self_auth.html")
//        binding.wvMain.loadUrl("http://dron.wellpage.co.kr/html_dev/notice_view.html?seq=46")

        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)
    }

    private fun checkStoragePermissions(callback: (Boolean) -> Unit) {
        permissionX.permissions(*startPermissions)
            .request { allGranted, grantedList, deniedList ->
                callback(allGranted)
            }
    }

    private fun checkBiometric(callback: (Boolean) -> Unit) {
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                initBiometric()
                callback(true)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Prompts the user to create credentials that your app accepts.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                        )
                    }
                    getBiometricResult.launch(enrollIntent)
                } else {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
                callback(false)
            }
            else -> {
                callback(false)
            }
        }
    }

    private fun initBiometric() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (BuildConfig.DEBUG) {
                    Toast.makeText(requireContext().applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }
                val sendData = webDataResponse.createWebParamData(WebDataCommand.AUTHENTICATE_BIOMETRICS, WebDataResult.FAIL)
                binding.wvMain.loadUrl("javascript:appToWeb('$sendData');")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (BuildConfig.DEBUG) {
                    Toast.makeText(requireContext().applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
                }
                val sendData = webDataResponse.createWebParamData(WebDataCommand.AUTHENTICATE_BIOMETRICS, WebDataResult.SUCCESS)
                binding.wvMain.loadUrl("javascript:appToWeb('$sendData');")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                if (BuildConfig.DEBUG) {
                    Toast.makeText(requireContext().applicationContext,
                        "Authentication failed", Toast.LENGTH_SHORT)
                        .show()
                }
                val sendData = webDataResponse.createWebParamData(WebDataCommand.AUTHENTICATE_BIOMETRICS, WebDataResult.FAIL)
                binding.wvMain.loadUrl("javascript:appToWeb('$sendData');")
            }
        })
    }

    /**
     * 바이오인증 시작
     */
    private fun onClickBiometricLogin(title: String, cancelTitle: String) {
        promptInfo = BiometricPrompt.PromptInfo
            .Builder()
            .setTitle(title)
            .setNegativeButtonText(cancelTitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setGateway() {
        mSdk.ad_setGatewayInfo(
            Constants.GATEWAY_TEST_URL,
            Constants.GATEWAY_TEST_ISSUER_URL,
            Constants.GATEWAY_TEST_VERIFIER_URL
        )
    }

    /**
     * 데이터베이스 백업, 복구에 쓰이는 마스터키 생성
     */
    private fun setMasterKey() {
        masterKey = mSdk.ad_createMasterKey()
        prefUtil.putStringSet(PreferenceKey.MASTER_KEY, masterKey?.toSet()!!)
    }

    private fun finishApp() {
        if (abs(System.currentTimeMillis() - backPressedTime) < 1000) {
            requireActivity().finish()
        } else {
            Toast.makeText(requireContext().applicationContext, "버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                .show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    inner class WebAppInterface {

        /**
         * 웹페이지에서 보낸 데이터 처리
         */
        @JavascriptInterface
        fun postMessage(message: String) {
            lifecycleScope.launch {
                var cmd = "error"
                val sendData: String? = try {
                    var param: JSONObject? = null
                    val receivedData: JSONObject

                    // 받은 데이터에서 명령값 추출
                    try {
                        receivedData = JSONObject(message)
                        cmd = receivedData.getString("cmd")
                    } catch (e: Exception) {
                        throw DroneException("moren.postMessage request fail")
                    }
                    // 받은 데이터에서 파라미터 추출
                    if (!receivedData.isNull("param") && receivedData.has("param")) {
                        param = receivedData.getJSONObject("param")
                    }

                    when (cmd) {
                        WebDataCommand.GET_DID_LIST -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_getDidList(0, 0)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GEN_DID -> {
                            withContext(ioDispatcher) {
                                val didName = param?.getString("didName")
                                val didDesc = param?.getString("didDesc")
                                val result = mSdk.ad_GenDid(didName, didDesc)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_DID -> {
                            withContext(ioDispatcher) {
                                val didId = param?.getString("didId")
                                val result = mSdk.ad_getDid(didId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_ISSUER_LIST -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_getIssuerList()
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_CREDENTIAL_LIST -> {
                            withContext(ioDispatcher) {
                                val issuerId = param?.getString("issuerId")
                                val result = mSdk.ad_getCredentialList(issuerId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_VC_LIST -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_getVcList(0, 0)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_VC -> {
                            withContext(ioDispatcher) {
                                val vcId = param?.getString("vcId")
                                val result = mSdk.ad_getVc(vcId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.SET_VC -> {
                            withContext(ioDispatcher) {
                                val vcId = param?.getString("vcId")
                                val inVC = param?.getString("inVC")
                                val result = mSdk.ad_setVc(vcId, inVC)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.DEL_VC -> {
                            withContext(ioDispatcher) {
                                val vcId = param?.getString("vcId")
                                val result = mSdk.ad_delVC(vcId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.BASIC_CREDENTIAL -> {
                            withContext(ioDispatcher) {
                                val issuerId = param?.getString("issuerId")
                                val vcTempId = param?.getString("vcTempId")
                                val didId = param?.getString("didId")
                                val inData = param?.getString("inData")
                                val result = mSdk.ad_BasicCredential(issuerId, vcTempId, didId, inData)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_BASIC_CREDENTIAL -> {
                            withContext(ioDispatcher) {
                                val result = webDataResponse.basicCredential()
                                if (result == null) {
                                    throw DroneException("fail")
                                }
                                else {
                                    webDataResponse.createWebDataByParams(cmd, result)
                                }
                            }
                        }
                        WebDataCommand.CREATE_BASIC_CREDENTIAL -> {
                            withContext(ioDispatcher) {
                                if (webDataResponse.basicCredential() != null) {
                                    throw DroneException("moren.postMessage request fail")
                                }

                                val didId = webDataResponse.basicDid() ?: throw DroneException("not found did")
                                val basicIssuerId = webDataResponse.basicIssuerId()
                                val name = param?.getString("name")
                                val birthDate = param?.getString("birthDate")
                                val telNo = param?.getString("telNo")
                                val telAgency = param?.getString("telAgency")
                                val ci = param?.getString("ci")
                                val di = param?.getString("di")

                                if (basicIssuerId.isNullOrEmpty()
                                    || name.isNullOrEmpty()
                                    || birthDate.isNullOrEmpty()
                                    || telNo.isNullOrEmpty()
                                    || telAgency.isNullOrEmpty()
                                    || ci.isNullOrEmpty()
                                    || di.isNullOrEmpty()) {
                                    throw DroneException("moren.postMessage request fail")
                                }

                                val credentialSubject = CredentialSubject.newInstance(
                                    name,
                                    birthDate,
                                    telNo,
                                    telAgency,
                                    ci,
                                    di)
                                val basicCredentialResult = mSdk.ad_BasicCredential(
                                    basicIssuerId,
                                    "KoreaPersonalBasicCredential",
                                    didId,
                                    mGson.toJson(credentialSubject)
                                )
                                webDataResponse.createWebDataByParams(cmd, basicCredentialResult)
                            }
                        }
                        WebDataCommand.IS_BIOMETRICS -> {
                            val biometricManager = BiometricManager.from(requireContext())
                            val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                                webDataResponse.createWebParamData(cmd, WebDataResult.SUCCESS)
                            }
                            else {
                                webDataResponse.createWebParamData(cmd, WebDataResult.FAIL)
                            }
                        }
                        WebDataCommand.AUTHENTICATE_BIOMETRICS -> {
                            checkBiometric {
                                if (it) {
                                    lifecycleScope.launch(mainDispatcher) {
                                        val description = param?.getString("description") ?: "지문 또는 얼굴인식이 필요합니다"
                                        val cancelTitle = param?.getString("cancelTitle") ?: "취소"
                                        onClickBiometricLogin(description, cancelTitle)
                                    }
                                }
                            }

                            null
                        }
                        WebDataCommand.GENERATE_VP -> {
                            withContext(ioDispatcher) {
                                val didId = webDataResponse.basicDid() ?: throw DroneException("not found did")
                                val vcId = param?.getString("vcId")
                                val result = mSdk.ad_genVP(didId, vcId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.LEAVE -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_leave()
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.SCAN_QRCODE_VERIFY -> {
                            permissionX.permissions(*cameraPermissions)
                                .request { allGranted, grantedList, deniedList ->
                                    if (allGranted) {
                                        getQrScanResult.launch(Intent(requireContext(), QrScannerActivity::class.java))
                                    }
                                    else {
                                        val sendData = webDataResponse.createWebParamData(cmd, WebDataResult.REQUEST_FAIL, "camera permission required")
                                        binding.wvMain.loadUrl("javascript:appToWeb('$sendData');")
                                    }
                                }
                            null
                        }
                        WebDataCommand.CLOUD_BACKUP -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_cloudBackup("G")
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.CLOUD_RESTORE -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_cloudRestore("G")
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.ADD_ZKP_KEY_DID -> {
                            withContext(ioDispatcher) {
                                val didId = param?.getString("didId")
                                val keyId = param?.getString("keyId")
                                val result = mSdk.ad_AddZKPkeyDid(didId, keyId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GET_DID_COUNT -> {
                            withContext(ioDispatcher) {
                                val result = mSdk.ad_getDidCount()
                                webDataResponse.createDidCountData(result)
                            }
                        }
                        WebDataCommand.CREATE_QRCODE -> {
                            withContext(calcDispatcher) {
                                val didId = webDataResponse.basicDid() ?: throw DroneException("not found did")
                                val url = param?.getString("url") ?: throw DroneException("not found url")
                                val currentDate = TimeUtils.getNowString(TimeUtils.getSafeDateFormat(
                                    QR_DATE_TYPE
                                ))
                                val qrcodeData = webDataResponse.generateQrcodeData(url, currentDate, didId)
                                val data = DroneWalletEncryption.getEncryptedQrText(qrcodeData) ?: throw DroneException("not acceptable")
                                webDataResponse.getCreateQrcode(data, currentDate)
                            }
                        }
                        WebDataCommand.BASIC_DID_ID -> {
                            withContext(ioDispatcher) {
                                val getDidListResult = mSdk.ad_getDidList(0, 0)
                                val getDidList = JSONObject(getDidListResult)
                                if (getDidList.getString("result") == WebDataResult.FAIL) {
                                    throw DroneException("not found did")
                                }
                                val didList = getDidList.getJSONArray("dids")

                                var didId: String? = null
                                for (index in 0 until didList.length()) {
                                    val did = didList.getJSONObject(index)
                                    if (did.getString("didName") == DidModel.BASIC_DID_NAME) {
                                        didId = did.getString("didId")
                                        break
                                    }
                                }

                                if (didId == null) {
                                    throw DroneException("not found did")
                                }
                                else {
                                    webDataResponse.getBasicDidIdResult(didId)
                                }
                            }
                        }
                        WebDataCommand.SET_VC_IMAGE -> {
                            withContext(ioDispatcher) {
                                val vcId = param?.getString("vcId")
                                val type = param?.getString("type")
                                val imageData = param?.getString("imageData")

                                if (vcId != null && type != null && type.length <= 5 && imageData != null) {
                                    val result = try {
                                        DroneWalletEncryption.saveEncryptedImage(
                                            requireContext(),
                                            vcId,
                                            type,
                                            imageData
                                        )
                                        true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        false
                                    }
                                    webDataResponse.setVCImage(result)
                                }
                                else {
                                    throw DroneException("fail")
                                }
                            }
                        }
                        WebDataCommand.GET_VC_IMAGE -> {
                            withContext(ioDispatcher) {
                                val vcId = param?.getString("vcId")
                                val type = param?.getString("type")
                                if (vcId != null && type != null && type.length <= 5) {
                                    val result = try {
                                        DroneWalletEncryption.loadEncryptedImage(
                                            requireContext(),
                                            vcId,
                                            type
                                        )
                                    } catch (e: Exception) {
                                        throw DroneException("fail")
                                    }
                                    webDataResponse.getVCImage(result)
                                }
                                else {
                                    throw DroneException("fail")
                                }
                            }
                        }
                        WebDataCommand.SET_VALUE -> {
                            withContext(calcDispatcher) {
                                val key = param?.getString("key") ?: throw DroneException("key not found")
                                // value 값이 존재하면 저장 없으면 삭제
                                if (param.has("value")) {
                                    val value = param.getString("value")
                                    prefUtil.putString(key, value)
                                }
                                else {
                                    prefUtil.removeValue(key)
                                }

                                val savedValue = prefUtil.getString(key)
                                webDataResponse.setValue(key, savedValue)
                            }
                        }
                        WebDataCommand.GET_VALUE -> {
                            withContext(calcDispatcher) {
                                val key = param?.getString("key") ?: throw DroneException("key not found")
                                val value = prefUtil.getString(key)
                                webDataResponse.getValue(key, value)
                            }
                        }
                        WebDataCommand.GET_ALL_JSONS -> {
                            withContext(ioDispatcher) {
                                webDataResponse.createWebDataByParams(cmd, mSdk.ad_getAllJsons())
                            }
                        }
                        WebDataCommand.GET_JSON -> {
                            withContext(ioDispatcher) {
                                val uuid = param?.getString("uuid") ?: throw DroneException("fail")
                                webDataResponse.createWebDataByParams(cmd, mSdk.ad_getJsonByUUID(uuid))
                            }
                        }
                        WebDataCommand.UPDATE_JSON -> {
                            withContext(ioDispatcher) {
                                val didId = webDataResponse.basicDid() ?: throw DroneException("not found did")
                                val jsonData = param?.getString("json") ?: throw DroneException("fail")
                                val uuid = param.getString("uuid") ?: throw DroneException("fail")
                                val result = mSdk.ad_updateJson(didId, jsonData, uuid)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.INSERT_JSON -> {
                            withContext(ioDispatcher) {
                                val didId = webDataResponse.basicDid() ?: throw DroneException("not found did")
                                val jsonData = param?.getString("json") ?: throw DroneException("fail")
                                val uuid = param.getString("uuid") ?: throw DroneException("fail")
                                val result = mSdk.ad_insertJson(didId, jsonData, uuid)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.DEL_JSON -> {
                            withContext(ioDispatcher) {
                                val uuid = param?.getString("uuid") ?: throw DroneException("fail")
                                val result = mSdk.ad_delJson(uuid)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GENERATE_QRCODE -> {
                            withContext(ioDispatcher) {
                                val didId = param?.getString("didId")
                                val url = param?.getString("url")
                                val result = mSdk.ad_generateQrcode(didId, url)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.GEN_VP -> {
                            withContext(ioDispatcher) {
                                val didId = param?.getString("didId")
                                val vcId = param?.getString("vcId")
                                val result = mSdk.ad_genVP(didId, vcId)
                                webDataResponse.createWebDataByParams(cmd, result)
                            }
                        }
                        WebDataCommand.EXIT -> {
                            finishApp()
                            null
                        }
                        else -> {
                            if (cmd == "null") {
                                cmd = "error"
                            }
                            throw DroneException("not found command")
                        }
                    }
                } catch (e: Exception) {
                    if (e is DroneException) {
                        webDataResponse.createWebParamData(cmd, WebDataResult.REQUEST_FAIL, e.message)
                    }
                    else {
                        webDataResponse.createWebParamData(cmd, WebDataResult.SDK_FAIL)
                    }
                }

//                Log.d("uuuu", "receiveMessage: $message")
//                Log.d("uuuu", "postMessage: $sendData")
                sendData?.let {
                    // sdk 에서 받은 데이터를 웹페이지에 전송
                    val replaced = encodeJsonHtml(sendData)
                    binding.wvMain.loadUrl("javascript:appToWeb('$replaced');")
                }
            }
        }
    }

}