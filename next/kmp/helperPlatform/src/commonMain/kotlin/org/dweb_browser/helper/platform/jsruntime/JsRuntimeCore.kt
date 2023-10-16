package org.dweb_browser.helper.platform.jsruntime

import com.shepeliev.webrtckmp.DataChannel
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.onIceCandidate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.helper.toUtf8ByteArray
import kotlin.time.Duration
import kotlin.time.measureTime

class JsRuntimeCore {
  val ws = JsRuntimeMessageChannel()
  private val totalSize = (1024 * 1024 * 1024 / 100).toInt()
  private val unitSize = (1024 * 63).toInt()

  private fun mbpres(size: Int, dur: Duration) =
    "${size / 1024 / 1024 / (dur.inWholeMilliseconds / 1000f)}mb/s"

  suspend fun testNative2Js(
  ) {
    var sendSize = 0;
    val dur = measureTime {
      ws.waitReady()
      while (sendSize < totalSize) {
        ws.postMessage(ByteArray(unitSize))
        sendSize += unitSize
      }
    }

    println("native2js:[$dur] ${mbpres(sendSize, dur)}")
  }

  suspend fun testJs2Native(
  ) {
    var gotSize = 0;
    val dur = measureTime {
      ws.waitReady()
      ws.postMessage("start-post")
      var lock = CompletableDeferred<Unit>()
      ws.onMessage { msg ->
        if (msg.text == "end-post") {
          lock.complete(Unit)
        }
        msg.binary?.let {
          gotSize += it.size
        }
      }
      lock.await()
    }
    println("js2native:[$dur] ${mbpres(gotSize, dur)}")
  }

  suspend fun testDuplex() {
    coroutineScope {
      val dur = measureTime {
        val job1 = async { testNative2Js() }
        val job2 = async { testJs2Native() }
        job1.await()
        job2.await()
      }
      println("duplex:[$dur]")
    }
  }

  val scope = CoroutineScope(ioAsyncExceptionHandler)

  suspend fun connectDataChannel(): DataChannel {
    val peer = PeerConnection()
    val dataChannel = peer.createDataChannel(
      "default",
      ordered = true,
      /** maxRetransmits 和 maxRetransmitTimeMs 不能同时配置 */
      maxRetransmits = 2,
//      maxRetransmitTimeMs = 3000
    )
      ?: throw Exception("fail to create data channel")
    val offer = peer.createOffer(OfferAnswerOptions())
//
//    dataChannel.onError.onEach {
//      println("data channel error:$it")
//    }.launchIn(scope)
//    dataChannel.onOpen.onEach {
//      println("data channel open")
//    }.launchIn(scope)
//    dataChannel.onClose.onEach {
//      println("data channel close")
//    }.launchIn(scope)
//    dataChannel.onMessage.onEach {
//      println("data channel message: ${it.size}")
//    }.launchIn(scope)
    peer.setLocalDescription(offer)
    var lock = CompletableDeferred<Unit>()
    ws.onMessage { msg ->
      if (msg.text?.startsWith("icecandidate:") == true) {
        val iceCandidateJson = msg.text.split(':', limit = 2)[1]
        val iceCandidate =
          Json { ignoreUnknownKeys = true }.decodeFromString<IceCandidateData>(iceCandidateJson)
            .toIceCandidate()
        peer.addIceCandidate(iceCandidate)
      } else if (msg.text?.startsWith("remote:") == true) {
        val des = Json.decodeFromString<SessionDescriptionData>(msg.text.split(':', limit = 2)[1])
          .toSessionDescription()
        peer.setRemoteDescription(des)
      } else if (msg.text == "open-channel") {
        lock.complete(Unit)
      }
    }
    ws.postMessage("data-channel:${Json.encodeToString(SessionDescriptionData.from(offer))}")
    peer.onIceCandidate.onEach { iceCandidate ->
      ws.postMessage("icecandidate:${Json.encodeToString(IceCandidateData.from(iceCandidate))}")
    }.launchIn(scope)
//    dataChannel.onOpen.first()
    lock.await()
    println("okk:${dataChannel.readyState}")
    return dataChannel
  }

  private val channel2Deferred by lazy { scope.async { connectDataChannel() } }

  suspend fun testNative2Js2() {
    val dataChannel = channel2Deferred.await()
    var sendSize = 0;
    println("Native2Js2: ${dataChannel.readyState}/${dataChannel.bufferedAmount}")
    val dur = measureTime {
      while (sendSize < totalSize) {
        dataChannel.send(ByteArray(unitSize))
        sendSize += unitSize
      }
      val lastMsg = "echo:${randomUUID()}".toUtf8ByteArray();
      dataChannel.send(lastMsg)
    }

    println("native2js2:[$dur] ${mbpres(sendSize, dur)}")
  }

  suspend fun testJs2Native2(
  ) {
    val dataChannel = channel2Deferred.await()
    var gotSize = 0;
    val endMessageContent = "end-channel-post".toUtf8ByteArray()
    val dur = measureTime {
      var lock = CompletableDeferred<Unit>()
      val job = dataChannel.onMessage.onEach { msg ->
        println("endMessageContentsize: ${msg.size}")
        if (msg.contentEquals(endMessageContent)) {
          lock.complete(Unit)
        } else {
          gotSize += msg.size
          dataChannel.send(gotSize.toLittleEndianByteArray())
        }
      }.launchIn(scope)
      ws.postMessage("start-channel-post")
      lock.await()
      job.cancel()
    }
    println("js2native2:[$dur] ${mbpres(gotSize, dur)}")
  }

  suspend fun connectReqResChannel(): ReqResChannel {
    ws.postMessage("start-req-res-channel")
    return ws.getReqResChannel()
  }

  private val reqResChannelDeferred by lazy { scope.async { connectReqResChannel() } }


  suspend fun testNative2Js3() {
    val reqResChannel = reqResChannelDeferred.await()
    var sendSize = 0;
    val dur = measureTime {
      while (sendSize < totalSize) {
        reqResChannel.send(ByteArray(unitSize))
        sendSize += unitSize
      }
      val lastMsg = "echo:${randomUUID()}".toUtf8ByteArray();
      reqResChannel.send(lastMsg)
    }

    println("native2js3:[$dur] ${mbpres(sendSize, dur)}")
  }

  suspend fun testJs2Native3() {
    val reqResChannel = reqResChannelDeferred.await()
    var gotSize = 0;
    val endMessageContent = "end-req-res-post".toUtf8ByteArray()
    val dur = measureTime {
      var lock = CompletableDeferred<Unit>()
      val job = reqResChannel.onMessage.onEach { msg ->
        if (msg.contentEquals(endMessageContent)) {
          lock.complete(Unit)
        }
        gotSize += msg.size
      }.launchIn(scope)
      ws.postMessage("start-req-res-post")
      lock.await()
      job.cancel()
    }
    println("js2native3:[$dur] ${mbpres(gotSize, dur)}")
  }


  suspend fun connectSuperChannel(): SuperChannel {
    ws.postMessage("start-super-channel")
    return ws.getSuperChannel()
  }

  private val superChannelDeferred by lazy { scope.async { connectSuperChannel() } }

  suspend fun testNative2Js4() {
    val superChannel = superChannelDeferred.await()
    var sendSize = 0;
    val endMsg = "echo:${randomUUID()}"
    val doneDeferred = CompletableDeferred<Unit>()
    superChannel.onMessage {
      if (it.text == endMsg) {
        doneDeferred.complete(Unit)
      }
    }
    val dur = measureTime {
      while (sendSize < totalSize) {
        superChannel.postMessage(ByteArray(unitSize))
        sendSize += unitSize
      }
      superChannel.postMessage(endMsg)
      println("native2js4 wait response")
      doneDeferred.await()
    }

    println("native2js4:[$dur] ${mbpres(sendSize, dur)}")
  }

  suspend fun testJs2Native4() {
    val superChannel = superChannelDeferred.await()
    var gotSize = 0;
    val dur = measureTime {
      var lock = CompletableDeferred<Unit>()
      superChannel.onMessage { msg ->
        if (msg.text == "end-super-post") {
          lock.complete(Unit)
          return@onMessage
        }
        when (val x = msg.binary) {
          null -> {}
          else -> {
            gotSize += x.size
          }
        }
      }
      ws.postMessage("start-super-post")
      lock.await()
    }
    println("js2native4:[$dur] ${mbpres(gotSize, dur)}")
  }
}

@Serializable
data class SessionDescriptionData(val type: String, val sdp: String) {
  companion object {
    fun from(des: SessionDescription) = SessionDescriptionData(des.type.name.lowercase(), des.sdp)
  }

  fun toSessionDescription() = SessionDescription(
    when (type) {
      "offer" -> SessionDescriptionType.Offer
      "pranswer" -> SessionDescriptionType.Pranswer
      "answer" -> SessionDescriptionType.Answer
      "rollback" -> SessionDescriptionType.Rollback
      else -> throw Exception(type)
    }, sdp
  )
}

@Serializable
data class IceCandidateData(
  val sdpMid: String, val sdpMLineIndex: Int, val candidate: String
) {
  companion object {
    fun from(can: IceCandidate) = IceCandidateData(can.sdpMid, can.sdpMLineIndex, can.candidate)
  }

  fun toIceCandidate() = IceCandidate(sdpMid, sdpMLineIndex, candidate)
}