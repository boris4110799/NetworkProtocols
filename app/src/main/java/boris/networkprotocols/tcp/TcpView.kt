package boris.networkprotocols.tcp

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import boris.networkprotocols.R
import boris.networkprotocols.ui.values.Orange
import java.net.Inet4Address

/**
 * Tcp screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TcpView(tcpViewModel : TcpViewModel, orientation : Int) {
	/**
	 * Store the UI state
	 */
	val state by tcpViewModel.uiState.collectAsStateWithLifecycle()
	
	/**
	 * Control textView's enabled property
	 */
	val enabled by remember(state.isListening, state.isConnecting) {
		mutableStateOf(!state.isListening && !state.isConnecting)
	}
	
	/**
	 * Control textField's expanded property
	 */
	var expanded by rememberSaveable { mutableStateOf(true) }
	
	val listState = rememberLazyListState()
	
	/**
	 * The message list
	 */
	val list by tcpViewModel.msgStateFlow.collectAsState()
	
	/**
	 * Store the state of the list size
	 */
	var sizeState by rememberSaveable { mutableIntStateOf(0) }
	
	val updateLocalPort : (String) -> Unit = { tcpViewModel.updateUIState(localPort = it) }
	val updateRemoteIP : (String) -> Unit = { tcpViewModel.updateUIState(remoteIP = it) }
	val updateRemotePort : (String) -> Unit = { tcpViewModel.updateUIState(remotePort = it) }
	val updateInputText : (String) -> Unit = { tcpViewModel.updateUIState(inputText = it) }
	val updateListening : (Boolean) -> Unit = {
		try {
			val port = state.localPort.toInt()
			tcpViewModel.setPort(port)
			tcpViewModel.changeListeningStatus(it)
			tcpViewModel.updateUIState(isLocalPortError = false, isListening = it)
		}
		catch (e : Exception) {
			tcpViewModel.updateUIState(isLocalPortError = true, isListening = false)
		}
	}
	val updateConnecting : (Boolean) -> Unit = {
		try {
			val inet4Address = Inet4Address.getByName(state.remoteIP)
			try {
				val port = state.remotePort.toInt()
				tcpViewModel.changeConnectingStatus(it, inet4Address, port)
				tcpViewModel.updateUIState(isRemoteIPError = false, isRemotePortError = false, isConnecting = it)
			}
			catch (e : Exception) {
				tcpViewModel.updateUIState(isRemoteIPError = false, isRemotePortError = true)
			}
		}
		catch (e : Exception) {
			tcpViewModel.updateUIState(isRemoteIPError = true)
		}
	}
	val deleteList = { tcpViewModel.deleteList() }
	val updateSend = { tcpViewModel.send(state.inputText) }
	
	//Scroll to bottommost of list when everytime list size has been changed
	LaunchedEffect(sizeState) {
		snapshotFlow { list.size }.collect {
			if (sizeState != list.size) listState.scrollToItem(sizeState, 0)
			sizeState = it
		}
	}
	
	Surface {
		Column {
			when (orientation) {
				Configuration.ORIENTATION_PORTRAIT  -> {
					Card(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
						if (expanded) {
							BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
								val spaceWidth = 8.dp
								val contentWidth = maxWidth.minus(spaceWidth.times(4)).div(4)
								Row(modifier = Modifier.height(IntrinsicSize.Min),
									verticalAlignment = Alignment.CenterVertically) {
									LocalPortView(state = { state }, enabled = { enabled },
										modifier = Modifier.width(contentWidth.times(2)).offset(spaceWidth),
										onValueChange = { updateLocalPort(it) })
									ListeningView(state = { state }, enabled = { !state.isConnecting },
										modifier = Modifier.width(contentWidth).offset(spaceWidth*2),
										onCheckedChange = { updateListening(it) })
									Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Expand",
										modifier = Modifier.width(contentWidth).aspectRatio(2f).offset(spaceWidth*3))
								}
							}
							BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
								val spaceWidth = 8.dp
								val contentWidth = maxWidth.minus(spaceWidth.times(4)).div(8)
								Row(modifier = Modifier.height(IntrinsicSize.Min),
									verticalAlignment = Alignment.CenterVertically) {
									RemoteIPView(state = { state }, enabled = { enabled },
										modifier = Modifier.width(contentWidth.times(4)).offset(spaceWidth),
										onValueChange = { updateRemoteIP(it) })
									RemotePortView(state = { state }, enabled = { enabled },
										modifier = Modifier.width(contentWidth.times(2)).offset(spaceWidth*2),
										onValueChange = { updateRemotePort(it) })
									ConnectingView(state = { state }, enabled = { !state.isListening },
										modifier = Modifier.width(contentWidth.times(2)).offset(spaceWidth*3),
										onCheckedChange = { updateConnecting(it) })
								}
							}
						}
						else {
							BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
								val spaceWidth = 8.dp
								val contentWidth = maxWidth.minus(spaceWidth.times(4)).div(3)
								Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
									horizontalArrangement = Arrangement.SpaceBetween,
									verticalAlignment = Alignment.CenterVertically) {
									ListeningView(state = { state }, enabled = { !state.isConnecting },
										modifier = Modifier.width(contentWidth),
										onCheckedChange = { updateListening(it) })
									ConnectingView(state = { state }, enabled = { !state.isListening },
										modifier = Modifier.width(contentWidth),
										onCheckedChange = { updateConnecting(it) })
									Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Expand",
										modifier = Modifier.aspectRatio(1f).offset(-spaceWidth))
								}
							}
						}
					}
					
					Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically) {
						Text(text = "訊息欄", modifier = Modifier.padding(start = 8.dp), fontSize = 24.sp)
						ClearIconView(modifier = Modifier.aspectRatio(1f).offset((-8).dp)) {
							deleteList()
						}
					}
					BoxWithConstraints(modifier = Modifier.padding(horizontal = 8.dp)) {
						val inputHeight = 70.dp
						val listHeight = maxHeight-inputHeight
						Column {
							LazyColumn(state = listState, contentPadding = PaddingValues(top = 4.dp),
								modifier = Modifier.height(listHeight)) {
								items(list, key = { item -> item.id }) {
									Row {
										Text(text = it.title, modifier = Modifier.weight(0.4f, true),
											style = MaterialTheme.typography.titleLarge)
										Text(text = it.msg, modifier = Modifier.weight(0.6f, true),
											style = MaterialTheme.typography.titleLarge)
									}
								}
							}
							BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(inputHeight),
								contentAlignment = Alignment.Center) {
								val sendWidth = 60.dp
								val spaceWidth = 8.dp
								val inputWidth = maxWidth-sendWidth-spaceWidth
								Row(modifier = Modifier.height(IntrinsicSize.Min),
									verticalAlignment = Alignment.CenterVertically) {
									InputView(state = { state }, modifier = Modifier.width(inputWidth),
										onValueChange = { updateInputText(it) })
									Spacer(modifier = Modifier.size(spaceWidth))
									SendIconView(modifier = Modifier.size(sendWidth)) { updateSend() }
								}
							}
						}
					}
				}
				
				Configuration.ORIENTATION_LANDSCAPE -> {
					BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
						val spaceWidth = 8.dp
						val clearWidth = 50.dp
						val contentWidth = maxWidth.minus(spaceWidth.times(7)).minus(clearWidth).div(5)
						Row(modifier = Modifier.height(70.dp), verticalAlignment = Alignment.CenterVertically) {
							LocalPortView(state = { state }, enabled = { enabled },
								modifier = Modifier.width(contentWidth).offset(spaceWidth),
								onValueChange = { updateLocalPort(it) })
							ListeningView(state = { state }, enabled = { !state.isConnecting },
								modifier = Modifier.width(contentWidth).offset(spaceWidth*2),
								onCheckedChange = { updateListening(it) })
							RemoteIPView(state = { state }, enabled = { enabled },
								modifier = Modifier.width(contentWidth).offset(spaceWidth*3),
								onValueChange = { updateRemoteIP(it) })
							RemotePortView(state = { state }, enabled = { enabled },
								modifier = Modifier.width(contentWidth).offset(spaceWidth*4),
								onValueChange = { updateRemotePort(it) })
							ConnectingView(state = { state }, enabled = { !state.isListening },
								modifier = Modifier.width(contentWidth).offset(spaceWidth*5),
								onCheckedChange = { updateConnecting(it) })
							ClearIconView(modifier = Modifier.size(clearWidth).offset(spaceWidth*6)) {
								deleteList()
							}
						}
					}
					BoxWithConstraints(modifier = Modifier.padding(horizontal = 8.dp)) {
						val inputHeight = 70.dp
						val listHeight = maxHeight-inputHeight
						Column {
							LazyColumn(state = listState, modifier = Modifier.height(listHeight)) {
								items(list, key = { item -> item.id }) {
									Row {
										Text(text = it.title, modifier = Modifier.weight(0.4f, true),
											style = MaterialTheme.typography.titleLarge)
										Text(text = it.msg, modifier = Modifier.weight(0.6f, true),
											style = MaterialTheme.typography.titleLarge)
									}
								}
							}
							BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(inputHeight),
								contentAlignment = Alignment.Center) {
								val sendWidth = 50.dp
								val spaceWidth = 8.dp
								val inputWidth = maxWidth-sendWidth-spaceWidth
								Row(modifier = Modifier.height(IntrinsicSize.Min),
									verticalAlignment = Alignment.CenterVertically) {
									InputView(state = { state }, modifier = Modifier.width(inputWidth),
										onValueChange = { updateInputText(it) })
									Spacer(modifier = Modifier.size(spaceWidth))
									SendIconView(modifier = Modifier.size(sendWidth)) { updateSend() }
								}
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun LocalPortView(state : () -> TcpState,
						  enabled : () -> Boolean,
						  modifier : Modifier,
						  onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state().localPort, onValueChange = { onValueChange(it) },
		label = { Text(text = "本機Port") }, enabled = enabled(), modifier = modifier, maxLines = 1,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state().isLocalPortError,
		supportingText = { if (state().isLocalPortError) Text(text = "Wrong") },
		colors = OutlinedTextFieldDefaults.colors(
			errorBorderColor = Color.Red,
			errorLabelColor = Color.Red,
			errorSupportingTextColor = Color.Red,
		))
}

@Composable
private fun RemoteIPView(state : () -> TcpState,
						 enabled : () -> Boolean,
						 modifier : Modifier,
						 onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state().remoteIP, onValueChange = { onValueChange(it) },
		label = { Text(text = "遠端IP") }, enabled = enabled(), modifier = modifier, maxLines = 1,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state().isRemoteIPError,
		supportingText = { if (state().isRemoteIPError) Text(text = "Wrong") },
		colors = OutlinedTextFieldDefaults.colors(
			errorBorderColor = Color.Red,
			errorLabelColor = Color.Red,
			errorSupportingTextColor = Color.Red,
		))
}

@Composable
private fun RemotePortView(state : () -> TcpState,
						   enabled : () -> Boolean,
						   modifier : Modifier,
						   onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state().remotePort, onValueChange = { onValueChange(it) },
		label = { Text(text = "遠端Port") }, enabled = enabled(), modifier = modifier, maxLines = 1,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state().isRemotePortError,
		supportingText = { if (state().isRemotePortError) Text(text = "Wrong") },
		colors = OutlinedTextFieldDefaults.colors(
			errorBorderColor = Color.Red,
			errorLabelColor = Color.Red,
			errorSupportingTextColor = Color.Red,
		))
}

@Composable
private fun InputView(state : () -> TcpState, modifier : Modifier, onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state().inputText, onValueChange = { onValueChange(it) },
		label = { Text(text = "請輸入內容") }, modifier = modifier, textStyle = TextStyle(fontSize = 20.sp),
		maxLines = 1)
}

@Composable
private fun ListeningView(state : () -> TcpState,
						  enabled : () -> Boolean,
						  modifier : Modifier,
						  onCheckedChange : (value : Boolean) -> Unit) {
	Row(modifier = modifier, horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically) {
		Text(text = "監聽")
		Spacer(modifier = Modifier.size(5.dp))
		Switch(enabled = enabled(), checked = state().isListening, onCheckedChange = { onCheckedChange(it) })
	}
}

@Composable
private fun ConnectingView(state : () -> TcpState,
						   enabled : () -> Boolean,
						   modifier : Modifier,
						   onCheckedChange : (value : Boolean) -> Unit) {
	Row(modifier = modifier, horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically) {
		Text(text = "連接")
		Spacer(modifier = Modifier.size(5.dp))
		Switch(enabled = enabled(), checked = state().isConnecting, onCheckedChange = { onCheckedChange(it) })
	}
}

@Composable
private fun ClearIconView(modifier : Modifier, onClick : () -> Unit) {
	IconButton(onClick = onClick, modifier = modifier) {
		Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_clear_all_24), contentDescription = null,
			modifier = Modifier.fillMaxHeight().aspectRatio(1f), tint = Orange)
	}
}

@Composable
private fun SendIconView(modifier : Modifier, onClick : () -> Unit) {
	IconButton(onClick = onClick, modifier = modifier) {
		Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_send_24), contentDescription = null,
			modifier = Modifier.fillMaxHeight().aspectRatio(1f), tint = Orange)
	}
}