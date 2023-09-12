package boris.networkprotocols.udp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class UdpViewModel : ViewModel() {
	private val _msgStateFlow = MutableStateFlow(ArrayList<Pair<String, String>>())
	val msgStateFlow = _msgStateFlow.asStateFlow()
	private val msgList = ArrayList<Pair<String, String>>()
	private var ds : DatagramSocket? = null
	private var port = 8888
	private var isServerOn = false
	private val tag = "UDP"
	
	fun addText(title : String, msg : String) {
		msgList.add(Pair(title, msg))
		_msgStateFlow.value = ArrayList(msgList)
	}
	
	fun deleteList() {
		msgList.clear()
		_msgStateFlow.value = ArrayList(msgList)
	}
	
	private fun startThread() {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				ds = DatagramSocket(port).apply { soTimeout = 5000 }
				Log.d(tag, "Server已啟動")
			}
			catch (e : SocketException) {
				Log.e(tag, "啟動失敗，原因: "+e.message)
				e.printStackTrace()
			}
			
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
							addText(dpRcv.address.hostAddress!!.toString(), string)
						}
					}
				}
				catch (e : IOException) {
					//e.printStackTrace()
				}
			}
		}
	}
	
	//切換伺服器監聽狀態
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
	
	//切換Port
	fun setPort(port : Int) {
		this.port = port
	}
	
	fun send(msg : String, remoteIP : InetAddress, remotePort : Int) {
		viewModelScope.launch(Dispatchers.IO) {
			Log.d(tag, "客户端IP：$remoteIP:$remotePort")
			val datagramSocket = DatagramSocket()
			val dpSend = DatagramPacket(msg.toByteArray(), msg.toByteArray().size, remoteIP, remotePort)
			datagramSocket.send(dpSend)
		}
	}
}