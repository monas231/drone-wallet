package kr.or.kotsa.tsdronewallet.ui.main

import kr.or.kotsa.tsdronewallet.constant.DidModel
import kr.or.kotsa.tsdronewallet.constant.WebDataCommand
import kr.or.kotsa.tsdronewallet.constant.WebDataResult
import com.markany.did_sdk.viewmodel.DidViewModel
import org.json.JSONObject

class WebDataResponse(private val mSdk: DidViewModel) {
    
    /**
     * 정상적인 리턴값 생성
     */
    fun createWebDataByParams(cmd: String, params: String): String {
        val webData = JSONObject()
        webData.put("cmd", cmd)
        webData.put("param", JSONObject(params))

        return webData.toString()
    }

    fun getScanQrcodeVerifyResult(url: String, didId: String): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.SCAN_QRCODE_VERIFY)
        val param = JSONObject()
        param.put("reason", "-")
        param.put("result", WebDataResult.SUCCESS)
        param.put("url", url)
        param.put("didId", didId)
        webData.put("param", param)

        return webData.toString()
    }

    fun getBasicDidIdResult(id: String): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.BASIC_DID_ID)
        val param = JSONObject()
        param.put("reason", "-")
        param.put("result", WebDataResult.SUCCESS)
        param.put("didId", id)
        webData.put("param", param)

        return webData.toString()
    }

    fun getCreateQrcode(data: String, date: String): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.CREATE_QRCODE)
        val param = JSONObject()
        param.put("data", data)
        param.put("date", date)
        webData.put("param", param)

        return webData.toString()
    }

    fun createDidCountData(count: Int): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.GET_DID_COUNT)
        val param = JSONObject()
        param.put("result", WebDataResult.SUCCESS)
        param.put("count", count)
        webData.put("param", param)

        return webData.toString()
    }

    /**
     * 비정상적인 리턴값 생성
     */
    fun createWebParamData(cmd: String, result: String, reason: String? = "-"): String {
        val webData = JSONObject()
        webData.put("cmd", cmd)
        val param = JSONObject()
        param.put("result", result)
        param.put("reason", reason)
        webData.put("param", param)

        return webData.toString()
    }

    fun basicDid(): String? {
        try {
            if (mSdk.ad_getDidCount() == 0) {
                val genDidResult = mSdk.ad_GenDid(DidModel.BASIC_DID_NAME, DidModel.BASIC_DID_DESC)
                val genDid = JSONObject(genDidResult)
                return genDid.getJSONObject("data").getJSONObject("didDoc")
                    .getJSONObject("did_document").getString("id")
            }

            val getDidListResult = mSdk.ad_getDidList(0, 0)
            val getDidList = JSONObject(getDidListResult)
            val didList = getDidList.getJSONArray("dids")

            var didId: String? = null
            for (index in 0 until didList.length()) {
                val did = didList.getJSONObject(index)
                if (did.getString("didName") == DidModel.BASIC_DID_NAME) {
                    didId = did.getString("didId")
                    break
                }
            }

            return didId
        } catch (e: Exception) {
            return null
        }
    }

    fun basicIssuerId(): String? {
        return try {
            val getIssuerListResult = mSdk.ad_getIssuerList()
            val getIssuerList = JSONObject(getIssuerListResult)
            val basicIssuer = getIssuerList.getJSONArray("data").getJSONObject(0)
            basicIssuer.getString("id")
        } catch (e: Exception) {
            null
        }
    }

    fun basicCredential(): String? {
        val vcListResultStr = mSdk.ad_getVcList(0, 0)
        val vcListResult = JSONObject(vcListResultStr)
        val vcList = vcListResult.getJSONArray("vcs")

        var result: String? = null
        for (index in 0 until vcList.length()) {
            val currentVc = vcList.getJSONObject(index)
            if (currentVc.getString("type") == "KoreaPersonalBasicCredential") {
                val vcId = currentVc.getString("vcId")
                val getVcResult = mSdk.ad_getVc(vcId)
                val getVc = JSONObject(getVcResult)
                if (getVc.getString("result") == "success") {
                    result = getVcResult
                    break
                }
            }
        }

        return result
    }

    fun generateQrcodeData(url: String, date: String, didId: String): String {
        val dateJson = JSONObject()
        dateJson.put("url", url)
        dateJson.put("date", date)
        dateJson.put("didId", didId)
        return dateJson.toString()
    }

    fun getVCImage(data: String): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.GET_VC_IMAGE)
        val param = JSONObject()
        param.put("reason", "-")
        param.put("result", WebDataResult.SUCCESS)
        param.put("imageData", data)
        webData.put("param", param)

        return webData.toString()
    }

    fun setVCImage(result: Boolean): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.SET_VC_IMAGE)
        val param = JSONObject()
        param.put("reason", "-")
        param.put("result", if (result) WebDataResult.SUCCESS else WebDataResult.FAIL)
        webData.put("param", param)

        return webData.toString()
    }

    fun setValue(key: String, value: String?): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.SET_VALUE)
        val param = JSONObject()
        param.put("result", WebDataResult.SUCCESS)
        param.put("reason", "-")
        param.put("key", key)
        value?.let {
            param.put("value", it)
        }
        webData.put("param", param)

        return webData.toString()
    }

    fun getValue(key: String, value: String?): String {
        val webData = JSONObject()
        webData.put("cmd", WebDataCommand.GET_VALUE)
        val param = JSONObject()
        param.put("result", WebDataResult.SUCCESS)
        param.put("reason", "-")
        param.put("key", key)
        value?.let {
            param.put("value", it)
        }
        webData.put("param", param)

        return webData.toString()
    }
    
}