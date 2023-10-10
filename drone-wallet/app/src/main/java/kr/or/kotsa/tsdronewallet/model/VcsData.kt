package kr.or.kotsa.tsdronewallet.model

data class VcsData(
    val vcId: String,
    val vcName: String,
    val issuerName: String,
    val vcDesc: String,
    val issuer: String,
    val type: String,
    val status: String
)
