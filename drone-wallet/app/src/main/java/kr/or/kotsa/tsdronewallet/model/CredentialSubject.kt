package kr.or.kotsa.tsdronewallet.model

class CredentialSubject private constructor(
    val personalBasic: PersonalBasic,
    val userInfo: UserInfo
) {
    val type = "internal"

    companion object {
        fun newInstance(
            name: String,
            birthDate: String,
            telNo: String,
            telAgency: String, ci: String,
            di: String
        ) = CredentialSubject(PersonalBasic(name, birthDate, telNo, telAgency), UserInfo(ci, di))
    }
}

data class PersonalBasic(
    val name: String,
    val birthDate: String,
    val telNo: String,
    val telAgency: String
)

data class UserInfo(
    val ci: String,
    val di: String
)
