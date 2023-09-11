package boris.networkprotocols

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import boris.networkprotocols.ui.values.MainTheme

class MainActivity: ComponentActivity() {
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MainView()
		}
	}
	
	@Composable
	@Preview()
	fun MainView() {
		MainTheme {
		
		}
	}
}