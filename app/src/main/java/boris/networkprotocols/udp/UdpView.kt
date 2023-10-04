package boris.networkprotocols.udp

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
 * Udp screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdpView(udpViewModel : UdpViewModel, orientation : Int) {
	/**
	 * Store the UI state
	 */
	val state by udpViewModel.uiState.collectAsStateWithLifecycle()
	
	/**
	 * Control textView's enabled property
	 */
	val enabled by remember(state.isListening) { mutableStateOf(!state.isListening) }
	
	val listState = rememberLazyListState()
	
	/**
	 * The message list
	 */
	val list : List<Pair<String, String>> by udpViewModel.msgStateFlow.collectAsState()
	
	/**
	 * Store the state of the list size
	 */
	var sizeState by rememberSaveable { mutableIntStateOf(0) }
	var expanded by rememberSaveable { mutableStateOf(true) }
	
	fun updateLocalPort(value : String) {
		udpViewModel.updateUIState(localPort = value)
	}
	
	fun updateSwitch(value : Boolean) {
		try {
			val port = state.localPort.toInt()
			udpViewModel.setPort(port)
			udpViewModel.changeServerStatus(value)
			udpViewModel.updateUIState(isLocalPortError = false, isListening = value)
		}
		catch (e : Exception) {
			udpViewModel.updateUIState(isLocalPortError = true, isListening = false)
		}
	}
	
	fun updateRemoteIP(value : String) {
		udpViewModel.updateUIState(remoteIP = value)
	}
	
	fun updateRemotePort(value : String) {
		udpViewModel.updateUIState(remotePort = value)
	}
	
	fun updateInputText(value : String) {
		udpViewModel.updateUIState(inputText = value)
	}
	
	fun deleteList() {
		udpViewModel.deleteList()
	}
	
	fun updateError() {
		try {
			val inet4Address = Inet4Address.getByName(state.remoteIP)
			udpViewModel.updateUIState(isRemoteIPError = false, isRemotePortError = try {
				val port = state.remotePort.toInt()
				udpViewModel.addMsg("發送", state.inputText)
				udpViewModel.send(state.inputText, inet4Address, port)
				false
			}
			catch (e : Exception) {
				true
			})
		}
		catch (e : Exception) {
			udpViewModel.updateUIState(isRemoteIPError = true)
		}
	}
	
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
								val contentWidth = maxWidth.minus(spaceWidth.times(3)).div(2)
								Row(modifier = Modifier.height(IntrinsicSize.Min),
									verticalAlignment = Alignment.CenterVertically) {
									LocalPortView(state = state, enabled = enabled,
										modifier = Modifier.width(contentWidth).offset(spaceWidth),
										onValueChange = { updateLocalPort(it) })
									SwitchView(state = state,
										modifier = Modifier.width(contentWidth.div(4).times(3)).offset(spaceWidth*2),
										onCheckedChange = { updateSwitch(it) })
									Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Expand",
										modifier = Modifier.width(contentWidth.div(4))
											.aspectRatio(1f)
											.offset(spaceWidth*2))
								}
							}
							BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
								val spaceWidth = 8.dp
								val contentWidth = maxWidth.minus(spaceWidth.times(3)).div(2)
								Row(modifier = Modifier.height(IntrinsicSize.Min),
									verticalAlignment = Alignment.CenterVertically) {
									RemoteIPView(state = state,
										modifier = Modifier.width(contentWidth).offset(spaceWidth),
										onValueChange = { updateRemoteIP(it) })
									RemotePortView(state = state,
										modifier = Modifier.width(contentWidth).offset(spaceWidth*2),
										onValueChange = { updateRemotePort(it) })
								}
							}
						}
						else {
							BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
								val spaceWidth = 8.dp
								val contentWidth = maxWidth.minus(spaceWidth.times(3)).div(2)
								Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
									horizontalArrangement = Arrangement.SpaceBetween,
									verticalAlignment = Alignment.CenterVertically) {
									SwitchView(state = state,
										modifier = Modifier.width(contentWidth).offset(spaceWidth),
										onCheckedChange = { updateSwitch(it) })
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
								items(list) {
									Row {
										Text(text = it.first, modifier = Modifier.weight(0.4f, true),
											style = MaterialTheme.typography.titleLarge)
										Text(text = it.second, modifier = Modifier.weight(0.6f, true),
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
									InputView(state = state, modifier = Modifier.width(inputWidth),
										onValueChange = { updateInputText(it) })
									Spacer(modifier = Modifier.size(spaceWidth))
									SendIconView(modifier = Modifier.size(sendWidth)) { updateError() }
								}
							}
						}
					}
				}
				
				Configuration.ORIENTATION_LANDSCAPE -> {
					BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
						val spaceWidth = 8.dp
						val clearWidth = 50.dp
						val contentWidth = maxWidth.minus(spaceWidth.times(6)).minus(clearWidth).div(4)
						Row(modifier = Modifier.height(70.dp), verticalAlignment = Alignment.CenterVertically) {
							LocalPortView(state = state, enabled = enabled,
								modifier = Modifier.width(contentWidth).offset(spaceWidth),
								onValueChange = { updateLocalPort(it) })
							SwitchView(state = state, modifier = Modifier.width(contentWidth).offset(spaceWidth*2),
								onCheckedChange = { updateSwitch(it) })
							RemoteIPView(state = state, modifier = Modifier.width(contentWidth).offset(spaceWidth*3),
								onValueChange = { updateRemoteIP(it) })
							RemotePortView(state = state, modifier = Modifier.width(contentWidth).offset(spaceWidth*4),
								onValueChange = { updateRemotePort(it) })
							ClearIconView(modifier = Modifier.size(clearWidth).offset(spaceWidth*5)) {
								deleteList()
							}
						}
					}
					BoxWithConstraints(modifier = Modifier.padding(horizontal = 8.dp)) {
						val inputHeight = 70.dp
						val listHeight = maxHeight-inputHeight
						Column {
							LazyColumn(state = listState, modifier = Modifier.height(listHeight)) {
								items(list) {
									Row {
										Text(text = it.first, modifier = Modifier.weight(0.4f, true),
											style = MaterialTheme.typography.titleLarge)
										Text(text = it.second, modifier = Modifier.weight(0.6f, true),
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
									InputView(state = state, modifier = Modifier.width(inputWidth),
										onValueChange = { updateInputText(it) })
									Spacer(modifier = Modifier.size(spaceWidth))
									SendIconView(modifier = Modifier.size(sendWidth)) { updateError() }
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
private fun LocalPortView(state : UdpState,
						  enabled : Boolean,
						  modifier : Modifier,
						  onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state.localPort, onValueChange = { onValueChange(it) },
		label = { Text(text = "本機Port") }, enabled = enabled, modifier = modifier, maxLines = 1,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.isLocalPortError,
		supportingText = { if (state.isLocalPortError) Text(text = "Wrong") },
		colors = OutlinedTextFieldDefaults.colors(
			errorBorderColor = Color.Red,
			errorLabelColor = Color.Red,
			errorSupportingTextColor = Color.Red,
		))
}

@Composable
private fun SwitchView(state : UdpState, modifier : Modifier, onCheckedChange : (value : Boolean) -> Unit) {
	Row(modifier = modifier, horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically) {
		Text(text = "監聽")
		Spacer(modifier = Modifier.size(5.dp))
		Switch(checked = state.isListening, onCheckedChange = { onCheckedChange(it) })
	}
}

@Composable
private fun RemoteIPView(state : UdpState, modifier : Modifier, onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state.remoteIP, onValueChange = { onValueChange(it) }, label = { Text(text = "遠端IP") },
		modifier = modifier, maxLines = 1, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
		isError = state.isRemoteIPError, supportingText = { if (state.isRemoteIPError) Text(text = "Wrong") },
		colors = OutlinedTextFieldDefaults.colors(
			errorBorderColor = Color.Red,
			errorLabelColor = Color.Red,
			errorSupportingTextColor = Color.Red,
		))
}

@Composable
private fun RemotePortView(state : UdpState, modifier : Modifier, onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state.remotePort, onValueChange = { onValueChange(it) },
		label = { Text(text = "遠端Port") }, modifier = modifier, maxLines = 1,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.isRemotePortError,
		supportingText = { if (state.isRemotePortError) Text(text = "Wrong") },
		colors = OutlinedTextFieldDefaults.colors(
			errorBorderColor = Color.Red,
			errorLabelColor = Color.Red,
			errorSupportingTextColor = Color.Red,
		))
}

@Composable
private fun InputView(state : UdpState, modifier : Modifier, onValueChange : (value : String) -> Unit) {
	OutlinedTextField(value = state.inputText, onValueChange = { onValueChange(it) },
		label = { Text(text = "請輸入內容") }, modifier = modifier, textStyle = TextStyle(fontSize = 20.sp),
		maxLines = 1)
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