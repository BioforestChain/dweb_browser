import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN
import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.util.UUID.randomUUID
import kotlin.jvm.optionals.getOrNull
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.seconds

class WebViewEchoTest {
  suspend fun doTestEcho(browser: Browser, useFix: Boolean) = coroutineScope {
    val engine = browser.engine()
    val mainFrame = browser.mainFrame().get()
    val jsWindow = mainFrame.executeJavaScript<JsObject>("window")!!

    val jsMessageChannel = mainFrame.executeJavaScript<JsObject>("new MessageChannel()")!!
    val jsPort1 = jsMessageChannel.property<JsObject>("port1").get()
    val jsPort2 = jsMessageChannel.property<JsObject>("port2").get()

    val messageChannel = Channel<String>(capacity = Channel.UNLIMITED)
    fun handleMessageEvent(event: JsObject) = runCatching {
      val ports = event.property<JsArray>("ports").get().also { jsPorts ->
        mutableListOf<JsObject>().apply {
          for (index in 0..<jsPorts.length()) {
            add(jsPorts.get<JsObject>(index)!!)
          }
        }
      }

      val message = event.property<Any>("data").getOrNull()
      val success = when (message) {
        is String -> {
          messageChannel.trySend(message)
          true
        }

        else -> {
          // should own type property, got js<->jvm error
          event.hasProperty("type")
        }
      }

      // release jsObject
      success
    }.getOrElse {
      false
    }

    if (useFix) {
      val randomName1 = randomUUID().toString()
      mainFrame.executeJavaScript<Unit>(
        """
        window["$randomName1"] = (port) => {
          port.addEventListener("message", (event)=>{
            while(true){
              if(port["$randomName1-message"](event)){
                break
              }else{
                console.log('retry', event.data)
              }
            }
          })
        }
      """.trimIndent()
      )
      jsWindow.call<Unit>(randomName1, jsPort1)
      jsWindow.removeProperty(randomName1)
      jsPort1.putProperty("$randomName1-message", JsFunctionCallback {
        val event = it[0] as JsObject
        val result = handleMessageEvent(event)
        event.close()
        result
      })
      jsPort1.call<Unit>("start")
    } else {
      val cb = JsFunctionCallback {
        handleMessageEvent(it[0] as JsObject)
      }
      jsPort1.call<Unit>("addEventListener", "message", cb)
      jsPort1.call<Unit>("start")
    }


    //--------------
    val randomName2 = randomUUID().toString()
    mainFrame.executeJavaScript<Unit>(
      """
        window["$randomName2"] = (port) => {
          port.addEventListener("message", (event) => {
            console.log(event.data)
            port.postMessage(event.data);
          });
          
          port.start()
        }
      """.trimIndent()
    )
    jsWindow.call<Unit>(randomName2, jsPort2)
    jsWindow.removeProperty(randomName2)


    /// send
    val actual = mutableListOf<String>()
    launch {
      jsPort1.call<Unit>("start")
      for (i in 1..5000) {
        val msg = "$i:${randomUUID()}"
        actual += msg
        jsPort1.call<Unit>("postMessage", msg)
      }
      // close js send and receive
      delay(10)
      jsPort1.call<Unit>("close")
      messageChannel.close()
    }

    // receive
    val expected = mutableListOf<String>()

    for (msg in messageChannel) {
      expected += msg
    }
    runCatching {
      assertContentEquals(expected, actual)
    }.getOrElse {
      println("useFix=$useFix $it")
      delay(10000000) // for debug in devtools
      throw it
    }
  }

  @Test
  fun testEchoBug() = runTest(timeout = 600.seconds) {
    withContext(Dispatchers.Default) {
      val engine = Engine.newInstance(EngineOptions.newBuilder(OFF_SCREEN).run {
        this.licenseKey(System.getProperty("jxbrowser.license.key"))
        build()
      })
      val browser = engine.newBrowser()
      browser.devTools().show()
      val useFix = true
      doTestEcho(browser, useFix)
      browser.close()
    }
  }
}