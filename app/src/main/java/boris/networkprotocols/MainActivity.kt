package boris.networkprotocols

import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import boris.networkprotocols.tcp.TcpView
import boris.networkprotocols.udp.UdpView
import boris.networkprotocols.udp.UdpViewModel
import boris.networkprotocols.ui.navigation.NavigationScreen
import boris.networkprotocols.ui.values.MainTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address

class MainActivity : ComponentActivity() {
	private val udpViewModel : UdpViewModel by viewModels()
	private var _localIPFlow = MutableStateFlow("")
	private var localIPFlow = _localIPFlow.asSharedFlow()
	
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MainView()
		}
		
		val connectivityManager = getSystemService(ConnectivityManager::class.java)
		connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network : Network) {
				super.onAvailable(network)
				for (i in connectivityManager.getLinkProperties(network)!!.linkAddresses) {
					lifecycleScope.launch {
						if (i.address is Inet4Address) _localIPFlow.emit(i.address.hostAddress!!)
					}
				}
			}
			
			override fun onLost(network : Network) {
				super.onLost(network)
				lifecycleScope.launch {
					_localIPFlow.emit("")
				}
			}
		})
	}
	
	@OptIn(ExperimentalMaterial3Api::class)
	@Composable
	fun MainView() {
		val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
		val scope = rememberCoroutineScope()
		val selectItem = remember { mutableStateOf("UDP") }
		val focusRequester = remember { FocusRequester() }
		val navController = rememberNavController()
		val localIP : String by localIPFlow.collectAsState(initial = "")
		
		MainTheme {
			ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
				ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.6f)) {
					NavigationDrawerItem(label = { Text("UDP") }, selected = selectItem.value == "UDP", onClick = {
						scope.launch { drawerState.close() }
						navController.navigate(NavigationScreen.UDP.name)
						selectItem.value = "UDP"
					})
					NavigationDrawerItem(label = { Text("TCP") }, selected = selectItem.value == "TCP", onClick = {
						scope.launch { drawerState.close() }
						navController.navigate(NavigationScreen.TCP.name)
						selectItem.value = "TCP"
					})
				}
			}) {
				Scaffold(modifier = Modifier.focusRequester(focusRequester).focusable(true), topBar = {
					TopAppBar(title = {
						Text(text = selectItem.value, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Justify,
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
						Column {
							Text(text = "本機IP: $localIP", modifier = Modifier.fillMaxWidth(), fontSize = 20.sp,
								textAlign = TextAlign.Center)
							
							NavHost(navController = navController, startDestination = NavigationScreen.UDP.name) {
								composable(NavigationScreen.UDP.name) {
									UdpView(udpViewModel)
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
}