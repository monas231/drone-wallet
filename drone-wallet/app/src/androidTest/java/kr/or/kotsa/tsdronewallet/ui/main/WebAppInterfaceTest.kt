package kr.or.kotsa.tsdronewallet.ui.main

import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.model.Atoms
import androidx.test.espresso.web.sugar.Web
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kr.or.kotsa.tsdronewallet.constant.Constants
import com.markany.did_sdk.viewmodel.DidViewModel
import kr.or.kotsa.tsdronewallet.ui.MainActivity
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class WebAppInterfaceTest {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()
    lateinit var sdk: DidViewModel
    lateinit var webDataResponse: WebDataResponse

    @Before
    fun setUp() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            sdk = DidViewModel.get(it, false)
            webDataResponse = WebDataResponse(sdk)
        }
    }

    @Test
    fun testWebAppInterface() {
        Web.onWebView()
            .forceJavascriptEnabled()
            .withNoTimeout()
            .check(WebViewAssertions.webMatches(Atoms.getCurrentUrl(), CoreMatchers.containsString(
                Constants.MAIN_URL)))

        val getDidList = "{\"cmd\":\"getDidList\"}"
//        Web.onWebView()
//            .check(
//                WebViewAssertions.webMatches(
//                    Atoms.script("window.moren.postMessage($getDidList)"), CoreMatchers.containsString("")
//                )
//            )
    }
}