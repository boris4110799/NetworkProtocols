package boris.networkprotocols

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import boris.networkprotocols.udp.UdpView
import boris.networkprotocols.udp.UdpViewModel
import boris.networkprotocols.ui.values.MainTheme

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
	@Test
	fun useAppContext() {
		// Context of the app under test.
		val appContext = InstrumentationRegistry.getInstrumentation().targetContext
		assertEquals("boris.networkprotocols", appContext.packageName)
	}
}


class MyStateRestorationTests {
	
	@get:Rule
	val composeTestRule = createComposeRule()
	
	@Test
	fun onRecreation_stateIsRestored() {
		val restorationTester = StateRestorationTester(composeTestRule)
		val udpViewModel = UdpViewModel()
		restorationTester.setContent {
			UdpView(udpViewModel = udpViewModel)
		}
		
		// TODO: Run actions that modify the state
		
		// Trigger a recreation
		restorationTester.emulateSavedInstanceStateRestore()
		
		// TODO: Verify that state has been correctly restored.
	}
}