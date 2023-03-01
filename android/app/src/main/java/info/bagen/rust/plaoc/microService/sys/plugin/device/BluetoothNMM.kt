package info.bagen.rust.plaoc.microService.sys.plugin.device

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.readByteArray
import info.bagen.rust.plaoc.microService.ipc.IpcStreamData
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.io.IOException
import java.util.*

inline fun debugBluetooth(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Bluetooth", tag, msg, err)

class BluetoothNMM : NativeMicroModule("bluetooth.sys.dweb") {

    companion object {
        const val BLUETOOTH_REQUEST = 10 // 请求打开蓝牙的状态标记
        const val BLUETOOTH_CAN_BE_FOUND = 11 // 启用可发现设备状态的状态标记
        val bluetoothOp = PromiseOut<String>() // 请求打开蓝牙的结果返回
        val bluetooth_found = PromiseOut<String>() //启用可发现设备状态的结果返回
        val findBluetoothResult = PromiseOut<MutableList<BluetoothTargets>>() // 查找蓝牙设备的结果返回
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val acceptThread = AcceptThread()

    override suspend fun _bootstrap() {
        apiRouting = routes(
            /** 请求打开蓝牙*/
            "/open" bind Method.GET to defineHandler { request ->
                var result = "Application for bluetooth rejected"
                // 如果蓝牙是关闭的
                if (bluetoothAdapter?.isEnabled == false && App.browserActivity != null) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    // 向用户请求启动蓝牙 （requestCode – 如果 >= 0，该代码将在活动退出时在 onActivityResult() 中返回，用于定位）
                    startActivityForResult(
                        App.browserActivity!!,
                        enableBtIntent,
                        BLUETOOTH_REQUEST,
                        null
                    )
                    result = bluetoothOp.waitPromise()
                }
                Response(Status.OK).body(result)
            },
            /** 关闭蓝牙*/
            "/close" bind Method.GET to defineHandler { request ->
                // 检测是否有权限
                if (ActivityCompat.checkSelfPermission(
                        App.appContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@defineHandler Response(Status.CONNECTION_REFUSED).body("Connection refused without permission")
                }
                bluetoothAdapter?.disable()
                Response(Status.OK)
            },
            /** 检测是否开启蓝牙*/
            "/check" bind Method.GET to defineHandler { request ->
                if (bluetoothAdapter == null) {
                    return@defineHandler Response(Status.INTERNAL_SERVER_ERROR).body("Device doesn't support Bluetooth")
                }
                val isEnabled = bluetoothAdapter.isEnabled
                val json = """{"isEnabled":${isEnabled}}"""
                Response(Status.OK).body(json)
            },
            /** 查找已连接的设备（查找已经连接的设备会更快）*/
            "/query" bind Method.GET to defineHandler { request ->
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                val result = mutableListOf<BluetoothTargets>()
                pairedDevices?.forEach { device ->
                    val name = device.name
                    val address = device.address // MAC address
                    result.add(BluetoothTargets(name, address, device.uuids[0].uuid))
                }
                bluetoothAdapter?.cancelDiscovery() // 取消正在进行的发现
                Response(Status.OK).body(JsonUtil.toJson(result))
            },
            /** 发现设备*/
            "/find" bind Method.GET to defineHandler { request ->
                val result = findBluetooth()
                bluetoothAdapter?.cancelDiscovery()// 取消正在进行的发现
                Response(Status.OK).body(result)
            },
            /** 连接设备
             * 作为客户端连接 /connect/client
             * 作为服务端连接 /connect/server
             * 关闭连接 /connect/close
             * */
            "/connect/{target}" bind Method.GET to defineHandler { request, ipc ->
                // 默认作为服务器连接
                val target = request.path("target") ?: "server"
                // 如果传递的是关闭消息
                if (target == "close") {
                    acceptThread.cancel()
                    return@defineHandler Response(Status.OK).body("Bluetooth socket closed")
                }
                try {
                    val strName = Query.string().required("name")(request)
                    val strUuid = Query.string().required("uuid")(request)
                    // 创建蓝牙socket对象
                    acceptThread.createServerSocket(strName, UUID.fromString(strUuid))
                    // 阻塞等待创建socket连接
                    acceptThread.start()
                    val socket = acceptThread.mySocket
                        ?: return@defineHandler Response(Status.INTERNAL_SERVER_ERROR).body("Bluetooth socket creation failed")
                    // 建立socket连接
//                    socket.connect()
                    ipc.postMessage(IpcStreamData.fromBinary(strName, socket.inputStream.readByteArray(), ipc))
                    ipc.onMessage {
                        // TODO 接收worker的消息并且发送信息
//                        val bufferedWriter =  socket.outputStream.bufferedWriter()
//                        bufferedWriter.write(it.message)
                    }
                } catch (e: Throwable) {
                    debugBluetooth("error connect: ${e.message}")
                    return@defineHandler Response(Status.INTERNAL_SERVER_ERROR).body(e.message.toString())
                }

                Response(Status.OK)
            },
            /** 启用可发现设备状态，最多5分钟,不然耗电*/
            "/canBeFound" bind Method.GET to defineHandler { request ->
                var time = Query.int().optional("time")(request) ?: 300
                if (time > 300) time = 300
                //启用可发现性
                val discoverableIntent: Intent =
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time)
                    }
                App.browserActivity?.let {
                    startActivityForResult(it, discoverableIntent, BLUETOOTH_CAN_BE_FOUND, null)
                    return@defineHandler Response(Status.OK).body("""{"canBeFound":"${bluetooth_found.waitPromise()}"}""")
                }
                Response(Status.OK).body("""{"canBeFound":"rejected"}""")
            },
        )
    }

    data class BluetoothTargets(val name: String, val address: String, val uuid: UUID? = null)

    /** 查找新的蓝牙设备*/
    private suspend fun findBluetooth(): String {
        if (App.browserActivity == null) {
            return "App initialization not completed"
        }
        // 发现设备时注册广播。
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(
            App.appContext,
            App.browserActivity!!.receiver,
            filter,
            ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS
        )

        registerReceiver(
            App.appContext,
            App.browserActivity!!.receiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND),
            ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS
        )
        return JsonUtil.toJson(findBluetoothResult.waitPromise())
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread() : Thread() {

        private var mmServerSocket: BluetoothServerSocket? = null
        var mySocket: BluetoothSocket? = null

        fun createServerSocket(DeviceName: String, myUUID: UUID) {
            if (ActivityCompat.checkSelfPermission(
                    App.appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO:权限处理
                return
            }
            mmServerSocket =
                bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(DeviceName, myUUID)
        }

        fun manageMyConnectedSocket(socket: BluetoothSocket) {
            mySocket = socket
        }


        override fun run() {
            // 继续监听直到发生异常或返回套接字。
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    debugBluetooth("Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // 关闭连接释放线程
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                debugBluetooth("Could not close the connect socket", e)
            }
        }
    }

    override suspend fun _shutdown() {
        // 检测是否有权限
        if (ActivityCompat.checkSelfPermission(
                App.appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter?.disable()
        }
    }
}