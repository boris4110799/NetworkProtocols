package boris.networkprotocols.tcp

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

data class TcpState(val localPort : String = "8888",
					val isLocalPortError : Boolean = false,
					val isListening : Boolean = false,
					val remoteIP : String = "192.168.0.101",
					val isRemoteIPError : Boolean = false,
					val remotePort : String = "8888",
					val isRemotePortError : Boolean = false,
					val isConnecting : Boolean = false,
					val inputText : String = "Hello")

data class TcpMessage(val id : Int, val title : String, val msg : String)

class TcpViewModel : ViewModel() {
	private val _uiState = MutableStateFlow(TcpState())
	val uiState : StateFlow<TcpState> = _uiState.asStateFlow()
	private val _msgState = MutableStateFlow(mutableStateListOf<TcpMessage>())
	val msgState = _msgState.asStateFlow()
	private val msgList = mutableStateListOf<TcpMessage>()
	private var server = TCPServer()
	private var client = TCPClient()
	private var port = 8888
	private val tag = "TCP"
	
	/**
	 * A function to update UI state
	 */
	fun updateUIState(localPort : String = uiState.value.localPort,
					  isLocalPortError : Boolean = uiState.value.isLocalPortError,
					  isListening : Boolean = uiState.value.isListening,
					  remoteIP : String = uiState.value.remoteIP,
					  isRemoteIPError : Boolean = uiState.value.isRemoteIPError,
					  remotePort : String = uiState.value.remotePort,
					  isRemotePortError : Boolean = uiState.value.isRemotePortError,
					  isConnecting : Boolean = uiState.value.isConnecting,
					  inputText : String = uiState.value.inputText) {
		_uiState.update {
			it.copy(localPort = localPort, isLocalPortError = isLocalPortError, isListening = isListening,
				remoteIP = remoteIP, isRemoteIPError = isRemoteIPError, remotePort = remotePort,
				isRemotePortError = isRemotePortError, isConnecting = isConnecting, inputText = inputText)
		}
	}
	
	/**
	 * Add a new message to list
	 */
	fun addMsg(title : String, msg : String) {
		msgList.add(TcpMessage(msgList.size, title, msg))
		viewModelScope.launch { _msgState.emit(msgList) }
	}
	
	/**
	 * Delete all message in list
	 */
	fun deleteList() {
		msgList.clear()
		viewModelScope.launch { _msgState.emit(mutableStateListOf()) }
	}
	
	/**
	 * Set up new port number
	 */
	fun setPort(port : Int) {
		this.port = port
	}
	
	/**
	 * Change the listening status
	 */
	fun changeListeningStatus(isOn : Boolean) {
		if (isOn) {
			if (!server.getStatus()) server.start()
		}
		else {
			viewModelScope.launch {
				server.close()
			}
		}
	}
	
	/**
	 * Change the connecting status
	 */
	fun changeConnectingStatus(isOn : Boolean, remoteIP : InetAddress, remotePort : Int) {
		if (isOn) {
			if (!client.getStatus()) client.start(remoteIP, remotePort)
		}
		else {
			viewModelScope.launch {
				client.close()
			}
		}
	}
	
	fun send(msg : String) {
		if (uiState.value.isListening) {
			server.sendMsg(msg)
		}
		else if (uiState.value.isConnecting) {
			client.sendMsg(msg)
		}
	}
	
	inner class TCPServer {
		private var serverSocket : ServerSocket? = null
		private var isServerRunning = false
		private var runningSocketMap = mutableMapOf<Socket, Boolean>()
		
		/** 啟動TCPServer */
		fun start() {
			viewModelScope.launch(Dispatchers.IO) {
				serverSocket = ServerSocket(port).apply {
					soTimeout = 5000
					reuseAddress = true
				}
				isServerRunning = true
				
				var socket : Socket
				var socketIP : String?
				while (isServerRunning) {
					Log.d(tag, "偵測裝置連接...")
					
					try {
						socket = serverSocket!!.accept().apply { soTimeout = 1000 }
						socketIP = socket.inetAddress.hostAddress
						Log.d(tag, "偵測到新裝置, IP: $socketIP")
						
						withContext(Dispatchers.Main) {
							addMsg("", "新裝置:$socketIP")
						}
						
						runningSocketMap[socket] = true
						startThread(socket, socketIP)
					}
					catch (e : IOException) {
						//e.printStackTrace()
					}
				}
			}
		}
		
		private fun startThread(socket : Socket, socketIP : String) {
			try {
				val bis = BufferedInputStream(socket.getInputStream())
				viewModelScope.launch(Dispatchers.IO) {
					val bytes = ByteArray(1024)
					var len : Int
					while (runningSocketMap[socket]!! && !socket.isClosed && !socket.isInputShutdown) {
						try {
							if (bis.read(bytes).also { len = it } > 0) {
								val receiveMsg = String(bytes, 0, len)
								Log.d(tag, "收到訊息: $receiveMsg")
								
								withContext(Dispatchers.Main) {
									addMsg(socketIP, receiveMsg)
								}
							}
							//When connection lost
							if (len == -1) {
								Log.d(tag, "連線已中斷")
								runningSocketMap[socket] = false
								withContext(Dispatchers.Main) {
									addMsg("", "${socketIP}連線已中斷")
								}
							}
						}
						catch (e : IOException) {
							//e.printStackTrace()
						}
					}
					runningSocketMap.remove(socket)
					socket.close()
				}
			}
			catch (e : IOException) {
				//e.printStackTrace()
			}
		}
		
		fun sendMsg(msg : String) {
			for (socket in runningSocketMap) {
				viewModelScope.launch(Dispatchers.IO) {
					try {
						val bos = BufferedOutputStream(socket.key.getOutputStream())
						bos.write(msg.toByteArray(StandardCharsets.UTF_8))
						bos.flush()
					}
					catch (e : IOException) {
						e.printStackTrace()
					}
				}
			}
			addMsg("發送", msg)
		}
		
		fun getStatus() : Boolean {
			return isServerRunning
		}
		
		suspend fun close() {
			isServerRunning = false
			coroutineScope {
				for (socket in runningSocketMap) {
					viewModelScope.launch {
						socket.setValue(false)
					}
				}
				delay(1000)
			}
			withContext(Dispatchers.IO) {
				serverSocket?.close()
			}
			serverSocket = null
		}
	}
	
	inner class TCPClient {
		private var socket : Socket? = null
		private var bis : BufferedInputStream? = null
		private var bos : BufferedOutputStream? = null
		private var isClientRunning = false
		
		fun start(remoteIP : InetAddress, remotePort : Int) {
			viewModelScope.launch(Dispatchers.IO) {
				try {
					socket = Socket(remoteIP, remotePort)
					socket!!.soTimeout = 2000
					isClientRunning = true
					
					withContext(Dispatchers.Main) {
						addMsg("", "連線成功!")
					}
					
					try {
						bis = BufferedInputStream(socket!!.getInputStream())
						try {
							bos = BufferedOutputStream(socket!!.getOutputStream())
							val bytes = ByteArray(1024)
							var len : Int
							while (isClientRunning && socket!!.isConnected) {
								try {
									if (bis!!.read(bytes).also { len = it } > 0) {
										val receiveMsg = String(bytes, 0, len)
										Log.d(tag, "收到: $receiveMsg")
										
										withContext(Dispatchers.Main) {
											addMsg(remoteIP.hostAddress!!, receiveMsg)
										}
									}
									if (len == -1) {
										Log.d(tag, "連線已中斷")
										isClientRunning = false
										withContext(Dispatchers.Main) {
											addMsg("", "連線已中斷")
											updateUIState(isConnecting = false)
										}
									}
								}
								catch (e : IOException) {
									//e.printStackTrace()
								}
							}
							try {
								if (bis != null) bis!!.close()
								if (bos != null) bos!!.close()
								if (socket != null) socket!!.close()
								Log.d(tag, "關閉Client")
							}
							catch (e : IOException) {
								e.printStackTrace()
							}
						}
						catch (e : IOException) {
							Log.d(tag, "BufferedOutputStream Error")
						}
					}
					catch (e : IOException) {
						Log.d(tag, "BufferedInputStream Error")
					}
				}
				catch (e : IOException) {
					e.printStackTrace()
					Log.d(tag, "無法連線")
					withContext(Dispatchers.Main) {
						addMsg("", "無法連線!")
						updateUIState(isConnecting = false)
					}
				}
			}
		}
		
		fun sendMsg(msg : String) {
			if (bos != null) {
				try {
					viewModelScope.launch(Dispatchers.IO) {
						bos!!.write(msg.toByteArray(StandardCharsets.UTF_8))
						bos!!.flush()
					}
					addMsg("發送", msg)
				}
				catch (e : IOException) {
					e.printStackTrace()
				}
			}
			else {
				addMsg("", "發送失敗!")
			}
		}
		
		fun getStatus() : Boolean {
			return isClientRunning
		}
		
		fun close() {
			isClientRunning = false
		}
	}
}