package kr.or.kotsa.tsdronewallet.model

import com.markany.did_sdk.data.DidData

data class DidResponse(
    override val result: String,
    override val code: String?,
    override val reason: String,
    override val startPosition: Int?,
    override val endPosition: Int?,
    override val totalCount: String?,
    val dids: List<DidData>?
) : ResultDelegate, SizeDelegate

data class VcsResponse(
    override val result: String,
    override val reason: String,
    override val code: String?,
    override val startPosition: Int,
    override val endPosition: Int,
    override val totalCount: String,
    val vcs: List<VcsData>?
) : ResultDelegate, SizeDelegate

data class JtiResponse(
    override val result: String,
    override val code: String?,
    override val reason: String,
    val jti: Int?
) : ResultDelegate

data class GenDidResponse(
    override val result: String,
    override val reason: String,
    override val code: String?,
    val data: DidDoc,
) : ResultDelegate

data class DidDoc(val didDoc: String)

data class VpResponse(
    override val result: String,
    override val reason: String,
    override val code: String?,
    val data: String
) : ResultDelegate

interface ResultDelegate {
    val result: String
    val reason: String
    val code: String?
}

interface SizeDelegate {
    val startPosition: Int?
    val endPosition: Int?
    val totalCount: String?
}

interface NameDelegate {
    val Name: String
    val filesize: String
}