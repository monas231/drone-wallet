package kr.or.kotsa.tsdronewallet.constant

object Constants {
    const val GATEWAY_URL = ""
    const val GATEWAY_ISSUER_URL = ""
    const val GATEWAY_VERIFIER_URL = ""

    const val GATEWAY_TEST_URL = "https://www.kotsa.or.kr/dcert/P1/"
    const val GATEWAY_TEST_ISSUER_URL = "https://www.kotsa.or.kr/dcert/P2/"
    const val GATEWAY_TEST_VERIFIER_URL = "https://www.kotsa.or.kr/dcert/P3/"

    const val MAIN_URL = "http://dron.wellpage.co.kr/"
    const val TEST_URL = "http://dron.wellpage.co.kr/test/index.html"

    const val QRCODE_URL = "https://15.164.155.124:8083/verify/vp/id"

    const val ISSUER_ID = "did:anyblockdid:b8e6631b6da0fbdcce26d6e412f2cb6995b5acdb"
}

object WebDataCommand {
    /**
     * 저장된 DID의 총 갯수를 반환하는 함수.
     */
    const val GET_DID_COUNT = "getDidCount"

    /**
     * 저장된 DID list 리턴
     */
    const val GET_DID_LIST = "getDidList"

    /**
     * DID 정보 리턴
     */
    const val GET_DID = "getDid"

    /**
     * DID 생성
     */
    const val GEN_DID = "GenDid"

    /**
     * Issuer 리스트 리턴
     */
    const val GET_ISSUER_LIST = "getIssuerList"

    /**
     * Issuer가 생성할수 있는 VC 리스트 리턴
     */
    const val GET_CREDENTIAL_LIST = "getCredentialList"

    /**
     * 기본증명서(VC) 발급요청
     */
    const val BASIC_CREDENTIAL = "BasicCredential"
    const val GET_VC = "getVc"

    /**
     * 저장된 VC 리스트 리턴
     */
    const val GET_VC_LIST = "getVcList"

    /**
     * VC를 월렛에 저장
     */
    const val SET_VC = "setVc"

    /**
     * VC 삭제
     */
    const val DEL_VC = "delVC"

    /**
     * VP 생성
     */
    const val GEN_VP = "genVP"

    /**
     * didId와 해당 didId로 VP를 제출할 url을 입력하여 QR 이미지로 리턴받는 함수.
     * QR이미지 내의 데이터는 암호화 된다.
     */
    const val GENERATE_QRCODE = "generateQrcode"

    /**
     * ‘/Download/[App Name]/backup/temp’ 경로에 파일을 업로드 한 후 App 에서 GoogleDriveServiceHelper 클래스를 사용하여 Google Drive 안의 지정된 폴더와 이름으로 백업파일을 업로드 한다.
     */
    const val CLOUD_BACKUP = "cloudBackup"
    const val CLOUD_RESTORE = "cloudRestore"

    /**
     * 기본 증명서 얻어오기
     */
    const val GET_BASIC_CREDENTIAL = "getBasicCredential"

    /**
     * 기본 증명서 발급, DID 를 최초 생성하고, 기본 증명서를 생성한다. 이미 기본증명서가 있다면 fail
     */
    const val CREATE_BASIC_CREDENTIAL = "createBasicCredential"

    /**
     * 생체인증 사용 가능 여부 확인
     */
    const val IS_BIOMETRICS = "isBiometrics"

    /**
     * 생체인증
     */
    const val AUTHENTICATE_BIOMETRICS = "authenticateBiometrics"

    /**
     * vcId값으로 wallet에서 VC를 검색하고, 서명하여 VP로 만든다.
     */
    const val GENERATE_VP = "generateVP"

    /**
     * DB 내용 초기화 함수.
     */
    const val LEAVE = "leave"

    /**
     * 증명서 qr 검증 요청
     */
    const val SCAN_QRCODE_VERIFY = "scanQrcodeVerify"

    /**
     * 입력된 DID에 대하여 영지식증명 키를 추가하는 함수
     * - DID키추가요청된DID를읽은후웰렛에서키를생성한후gatewaydid영지식증명키추가함수를
     * 이용하여 키를 추가한다. 키가 추가된 DID를 웰렛에 업데이트한다.
     * - String “ keyId = didId + #bls + jti ” 형태이다
     * - 동일한 사용자 didId에 동일한 key id를 등록할 수 없다
     * - 동일한 사용자 didId에 키를 추가할 때는 jti는 최초 1부터 시작하여야 하며, 추가 성공시
     * 연속적으로 +1 증가해야 한다.
     * - keyId를 ""으로 빈값을 넣어주면 새로운 keyId를 생성해서 추가하고
     * - keyId에 "did:anyblockdid:8fdf9d203f3f4401230886ac554868b8ce95afde#bls1"처럼 값을 넣어서
     * 호출하면 DB에서 keyId에 해당하는 publickey를 찾아서 추가하게 된다.
     * - 따라서 keyId에 값을 넣어서 호출했으나 DB에 해당 keyId에 해당하는 publickey를 찾을수 없다면
     * {
     * "result" : "fail",
     * "reason" : "Does Not Search Pubkey by keyId"
     * }
     * 과 같은 결과가 리턴된다.
     * 샘플에서 keyId 값을 "" 로 해서 사용함
     */
    const val ADD_ZKP_KEY_DID = "AddZKPkeyDid"
    const val CREATE_QRCODE = "createQrcode"
    const val BASIC_DID_ID = "basicDidId"
    const val SET_VC_IMAGE = "setVCImage"
    const val GET_VC_IMAGE = "getVCImage"
    const val SET_VALUE = "setValue"
    const val GET_VALUE = "getValue"
    const val GET_ALL_JSONS = "getAllJsons"
    const val GET_JSON = "getJson"
    const val UPDATE_JSON = "updateJson"
    const val INSERT_JSON = "insertJson"
    const val DEL_JSON = "delJson"
    const val EXIT = "exit"
}

object WebDataResult {
    const val SUCCESS = "success"
    /**
     * 실패. SDK 에서 null을 리턴하거나  Exception 발생 등 SDK에서 에러 발생 시
     */
    const val SDK_FAIL = "sdk_fail"

    /**
     * 요청(webToApp) message 가 잘못됐을 경우
     * webToApp 에서 보내는 message 가 json 파싱 자체가 안될 경우나 cmd 값을 알 수 없는 경우에는 cmd 값에 error 값을 넣어 전송
     */
    const val REQUEST_FAIL = "request_fail"
    const val FAIL = "fail"
}

object DidModel {
    const val BASIC_DID_NAME = "basicDidName"
    const val BASIC_DID_DESC = "basicDidDesc"
}