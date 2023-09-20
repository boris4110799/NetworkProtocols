package boris.networkprotocols.udp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

data class UdpState(val localPort : String = "8888",
					val isLocalPortError : Boolean = false,
					val isListening : Boolean = false,
					val remoteIP : String = "192.168.0.101",
					val isRemoteIPError : Boolean = false,
					val remotePort : String = "8888",
					val isRemotePortError : Boolean = false,
					val inputText : String = "Hello")

class UdpViewModel : ViewModel() {
	private val _uiState = MutableStateFlow(UdpState())
	val uiState : StateFlow<UdpState> = _uiState.asStateFlow()
	private val _msgStateFlow = MutableStateFlow(arrayListOf<Pair<String, String>>())
	val msgStateFlow = _msgStateFlow.asStateFlow()
	private val msgList = arrayListOf<Pair<String, String>>()
	private var ds : DatagramSocket? = null
	private var port = 8888
	private var isServerOn = false
	private val tag = "UDP"
	
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
					  inputText : String = uiState.value.inputText) {
		_uiState.update {
			it.copy(localPort = localPort, isLocalPortError = isLocalPortError, isListening = isListening,
				remoteIP = remoteIP, isRemoteIPError = isRemoteIPError, remotePort = remotePort,
				isRemotePortError = isRemotePortError, inputText = inputText)
		}
	}
	
	/**
	 * Add a new message to list
	 */
	fun addMsg(title : String, msg : String) {
		msgList.add(Pair(title, msg))
		_msgStateFlow.value = ArrayList(msgList)
	}
	
	/**
	 * Delete all message in list
	 */
	fun deleteList() {
		msgList.clear()
		_msgStateFlow.value = ArrayList(msgList)
	}
	
	/**
	 * Start a coroutine for running udp logic
	 */
	private fun startThread() {
		viewModelScope.launch(Dispatchers.IO) {
			//Create a socket
			try {
				ds = DatagramSocket(port).apply { soTimeout = 5000 }
				Log.d(tag, "Server已啟動")
			}
			catch (e : SocketException) {
				Log.e(tag, "啟動失敗，原因: "+e.message)
				e.printStackTrace()
			}
			
			//Continuously collect the incoming message
			val msgRcv = ByteArray(1024)
			val dpRcv = DatagramPacket(msgRcv, msgRcv.size)
			while (isServerOn) {
				Log.d(tag, "Server監聽中..")
				try {
					if (ds != null) {
						ds!!.receive(dpRcv)
						val string = String(dpRcv.data, dpRcv.offset, dpRcv.length)
						Log.d(tag, "收到資料： $string")
						
						withContext(Dispatchers.Main) {
							addMsg(dpRcv.address.hostAddress!!.toString(), string)
						}
					}
				}
				catch (e : IOException) {
					//e.printStackTrace()
				}
			}
		}
	}
	
	/**
	 * Change the server status
	 */
	fun changeServerStatus(isOn : Boolean) {
		isServerOn = isOn
		if (isOn && ds == null) {
			startThread()
		}
		else if (!isOn && ds != null) {
			ds!!.close()
			ds = null
			Log.d(tag, "Server已關閉")
		}
	}
	
	/**
	 * Set up new port number
	 */
	fun setPort(port : Int) {
		this.port = port
	}
	
	/**
	 * Create a socket and send a message
	 */
	fun send(msg : String, remoteIP : InetAddress, remotePort : Int) {
		viewModelScope.launch(Dispatchers.IO) {
			Log.d(tag, "客户端IP：$remoteIP:$remotePort")
			val datagramSocket = DatagramSocket()
			val dpSend = DatagramPacket(msg.toByteArray(), msg.toByteArray().size, remoteIP, remotePort)
			datagramSocket.send(dpSend)
		}
	}
}