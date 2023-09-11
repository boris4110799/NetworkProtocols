package boris.networkprotocols

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import boris.networkprotocols.tcp.TcpView
import boris.networkprotocols.udp.UdpView
import boris.networkprotocols.ui.navigation.NavigationScreen
import boris.networkprotocols.ui.values.MainTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MainView()
		}
	}
	
	@OptIn(ExperimentalMaterial3Api::class)
	@Composable
	@Preview(showBackground = true)
	fun MainView() {
		val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
		val scope = rememberCoroutineScope()
		val selectItem = remember { mutableStateOf("UDP") }
		val focusRequester = remember { FocusRequester() }
		val navController = rememberNavController()
		
		MainTheme {
			ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
				ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.6f)) {
					NavigationDrawerItem(label = { Text("UDP") }, selected = selectItem.value == "UDP", onClick = {
						scope.launch { drawerState.close() }
						selectItem.value = "UDP"
					})
					NavigationDrawerItem(label = { Text("TCP") }, selected = selectItem.value == "TCP", onClick = {
						scope.launch { drawerState.close() }
						selectItem.value = "TCP"
					})
				}
			}) {
				Scaffold(modifier = Modifier.focusRequester(focusRequester).focusable(true), topBar = {
					TopAppBar(title = {
						Text(text = "AppBar", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Justify,
							color = Color.White)
					}, navigationIcon = {
						IconButton(onClick = { scope.launch { drawerState.open() } }) {
							Icon(Icons.Filled.Menu, contentDescription = "Localized description")
						}
					}, colors = TopAppBarDefaults.topAppBarColors(Color.Gray, Color.Gray, Color.White, Color.White,
						Color.White))
				}) { innerPadding ->
					Surface(modifier = Modifier.padding(innerPadding), shape = MaterialTheme.shapes.small,
						color = MaterialTheme.colorScheme.background) {
						NavHost(navController = navController, startDestination = NavigationScreen.UDP.name) {
							composable(NavigationScreen.UDP.name) {
								UdpView()
							}
							composable(NavigationScreen.TCP.name) {
								TcpView()
							}
						}
					}
				}
			}
		}
	}
}