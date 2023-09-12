package boris.networkprotocols.udp

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boris.networkprotocols.R
import boris.networkprotocols.ui.values.Orange
import java.net.Inet4Address

@Composable
fun UdpView(udpViewModel : UdpViewModel) {
	var localPort by rememberSaveable { mutableStateOf("8888") }
	var isLocalPortError by remember { mutableStateOf(false) }
	var isListening by rememberSaveable { mutableStateOf(false) }
	var remoteIP by rememberSaveable { mutableStateOf("192.168.0.102") }
	var isRemoteIPError by remember { mutableStateOf(false) }
	var remotePort by rememberSaveable { mutableStateOf("8888") }
	var isRemotePortError by remember { mutableStateOf(false) }
	var inputText by rememberSaveable { mutableStateOf("Hello") }
	val list : List<Pair<String, String>> by udpViewModel.msgStateFlow.collectAsState()
	
	Surface {
		Column {
			Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
				OutlinedTextField(value = localPort, onValueChange = { localPort = it },
					label = { Text(text = "本機Port") }, modifier = Modifier.requiredWidthIn(180.dp, 200.dp),
					maxLines = 1, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					isError = isLocalPortError, supportingText = { if (isLocalPortError) Text(text = "Wrong") },
					colors = OutlinedTextFieldDefaults.colors(
						errorBorderColor = Color.Red,
						errorLabelColor = Color.Red,
						errorSupportingTextColor = Color.Red,
					))
				Row(modifier = Modifier.requiredWidthIn(180.dp, 200.dp).fillMaxHeight(),
					horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
					Text(text = "監聽")
					Spacer(modifier = Modifier.size(5.dp))
					Switch(checked = isListening, onCheckedChange = {
						try {
							val port = localPort.toInt()
							udpViewModel.setPort(port)
							udpViewModel.changeServerStatus(it)
							isListening = it
							isLocalPortError = false
						}
						catch (e : Exception) {
							isListening = false
							isLocalPortError = true
						}
					})
				}
			}
			Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
				OutlinedTextField(value = remoteIP, onValueChange = { remoteIP = it },
					label = { Text(text = "遠端IP") },
					modifier = Modifier.requiredWidthIn(180.dp, 200.dp).fillMaxHeight(), maxLines = 1,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = isRemoteIPError,
					supportingText = { if (isRemoteIPError) Text(text = "Wrong") },
					colors = OutlinedTextFieldDefaults.colors(
						errorBorderColor = Color.Red,
						errorLabelColor = Color.Red,
						errorSupportingTextColor = Color.Red,
					))
				OutlinedTextField(value = remotePort, onValueChange = { remotePort = it },
					label = { Text(text = "遠端Port") },
					modifier = Modifier.requiredWidthIn(180.dp, 200.dp).fillMaxHeight(), maxLines = 1,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = isRemotePortError,
					supportingText = { if (isRemotePortError) Text(text = "Wrong") },
					colors = OutlinedTextFieldDefaults.colors(
						errorBorderColor = Color.Red,
						errorLabelColor = Color.Red,
						errorSupportingTextColor = Color.Red,
					))
			}
			Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
				Text(text = "訊息欄", modifier = Modifier.requiredWidthIn(180.dp, 200.dp), fontSize = 24.sp)
				Row(modifier = Modifier.requiredWidthIn(180.dp, 200.dp).fillMaxHeight(),
					horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
					IconButton(onClick = { udpViewModel.deleteList() }, modifier = Modifier.aspectRatio(1f)) {
						Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_clear_all_24),
							contentDescription = null, modifier = Modifier.fillMaxHeight().aspectRatio(1f),
							tint = Orange)
					}
				}
			}
			BoxWithConstraints(modifier = Modifier.padding(horizontal = 8.dp)) {
				val inputHeight = 70.dp
				val listHeight = maxHeight-inputHeight
				Column {
					LazyColumn(state = rememberLazyListState(), contentPadding = PaddingValues(top = 4.dp),
						modifier = Modifier.height(listHeight)) {
						item {
							list.forEach {
								Row {
									Text(text = it.first, modifier = Modifier.weight(0.4f, true),
										style = MaterialTheme.typography.titleLarge)
									Text(text = it.second, modifier = Modifier.weight(0.6f, true),
										style = MaterialTheme.typography.titleLarge)
								}
							}
						}
					}
					Row(modifier = Modifier.fillMaxWidth().height(inputHeight),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween) {
						OutlinedTextField(value = inputText, onValueChange = { inputText = it },
							label = { Text(text = "請輸入內容") },
							modifier = Modifier.requiredWidthIn(250.dp, 350.dp).fillMaxHeight(),
							textStyle = TextStyle(fontSize = 20.sp), maxLines = 1)
						IconButton(onClick = {
							try {
								val inet4Address = Inet4Address.getByName(remoteIP)
								isRemoteIPError = false
								isRemotePortError = try {
									val port = remotePort.toInt()
									udpViewModel.addText("發送", inputText)
									udpViewModel.send(inputText, inet4Address, port)
									false
								}
								catch (e : Exception) {
									true
								}
							}
							catch (e : Exception) {
								isRemoteIPError = true
							}
						}, modifier = Modifier.size(60.dp)) {
							Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_send_24),
								contentDescription = null, modifier = Modifier.fillMaxHeight().aspectRatio(1f),
								tint = Orange)
						}
					}
				}
			}
		}
	}
}