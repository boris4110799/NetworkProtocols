package boris.networkprotocols

import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address

class MainActivity : ComponentActivity() {
	private val udpViewModel : UdpViewModel by viewModels()
	private var _localIPFlow = MutableStateFlow("")
	private var localIPFlow = _localIPFlow.asStateFlow()
	
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
		
		onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(false) {
			override fun handleOnBackPressed() {
				Log.d("test", "123")
			}
		})
	}
	
	
	@OptIn(ExperimentalMaterial3Api::class)
	@Composable
	fun MainView() {
		val navController = rememberNavController()
		val scope = rememberCoroutineScope()
		val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
		val focusRequester = remember { FocusRequester() }
		var selectItem by remember { mutableStateOf("UDP") }
		val handleBackHandler = remember(selectItem) { false }
		val localIP : String by localIPFlow.collectAsState(initial = "")
		
		var orientation by remember { mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT) }
		val configuration = LocalConfiguration.current
		
		navController.setLifecycleOwner(LocalLifecycleOwner.current)
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) BackInvokeHandler(handleBackHandler)
		else BackHandler(handleBackHandler) {}
		
		LaunchedEffect(configuration) {
			// Save any changes to the orientation value on the configuration object
			snapshotFlow { configuration.orientation }.collect { orientation = it }
		}
		
		MainTheme {
			ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
				ModalDrawerSheet(modifier = Modifier.fillMaxWidth(when (orientation) {
					Configuration.ORIENTATION_PORTRAIT  -> 0.4f
					Configuration.ORIENTATION_LANDSCAPE -> 0.3f
					else                                -> 0.4f
				})) {
					NavigationDrawerItem(label = { Text("UDP") }, selected = selectItem == "UDP", onClick = {
						scope.launch { drawerState.close() }
						navController.navigate(NavigationScreen.UDP.name) {
							launchSingleTop = true
							popUpTo(navController.currentBackStack.value[1].destination.id) {
								inclusive = true
								saveState = true
							}
							restoreState = true
						}
						selectItem = "UDP"
					})
					NavigationDrawerItem(label = { Text("TCP") }, selected = selectItem == "TCP", onClick = {
						scope.launch { drawerState.close() }
						navController.navigate(NavigationScreen.TCP.name) {
							launchSingleTop = true
							popUpTo(navController.currentBackStack.value[1].destination.id) {
								inclusive = true
								saveState = true
							}
							restoreState = true
						}
						selectItem = "TCP"
					})
				}
			}) {
				Scaffold(modifier = Modifier.focusRequester(focusRequester).focusable(true), topBar = {
					TopAppBar(title = {
						Text(text = selectItem, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Justify,
							color = Color.White)
					}, navigationIcon = {
						IconButton(onClick = { scope.launch { drawerState.open() } }) {
							Icon(Icons.Filled.Menu, contentDescription = "Localized description")
						}
					}, colors = TopAppBarDefaults.topAppBarColors(Color.Gray, Color.Gray, Color.White, Color.White,
						Color.White))
				}) { innerPadding ->
					Column(modifier = Modifier.padding(innerPadding)) {
						Text(text = "本機IP: $localIP", modifier = Modifier.fillMaxWidth(), fontSize = 20.sp,
							textAlign = TextAlign.Center)
						NavHost(navController = navController, startDestination = NavigationScreen.UDP.name) {
							composable(NavigationScreen.UDP.name) {
								UdpView(udpViewModel, orientation)
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
	
	@Composable
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	fun BackInvokeHandler(handleBackHandler : Boolean,
						  priority : Int = OnBackInvokedDispatcher.PRIORITY_DEFAULT,
						  callback : () -> Unit = {}) {
		val backInvokedCallback = remember {
			OnBackInvokedCallback {
				callback()
			}
		}
		
		val activity = if (LocalLifecycleOwner.current is MainActivity) LocalLifecycleOwner.current as MainActivity
		else if (LocalContext.current is MainActivity) LocalContext.current as MainActivity
		else throw IllegalStateException("Fail to fetch MainActivity")
		
		if (handleBackHandler) {
			activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(priority, backInvokedCallback)
		}
		
		LaunchedEffect(handleBackHandler) {
			if (!handleBackHandler) {
				activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backInvokedCallback)
			}
		}
		
		DisposableEffect(activity.lifecycle, activity.onBackInvokedDispatcher) {
			onDispose {
				activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backInvokedCallback)
			}
		}
	}
}