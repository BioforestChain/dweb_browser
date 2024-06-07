package info.bagen.dwebbrowser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointLifecycleInit
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcLifecycle
import org.dweb_browser.core.ipc.helper.IpcLifecycleInit
import org.dweb_browser.core.ipc.helper.endpointMessageToJson
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals


@Serializable
sealed interface Base {}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
@JsonClassDiscriminator("state")
sealed class AA(val a: String) : Base {

}

@Serializable
@SerialName("2")
class BB(val b: String) : Base {}

@Serializable
@SerialName("c")
class AAC(val c: String) : AA("c") {}

@Serializable
@SerialName("d")
class AAD(val d: String) : AA("d") {}


class CommonEndpointTest {
  @Test
  fun testCode() = runCommonTest {
    val demoModule = SerializersModule {
      polymorphic(AA::class) {
        subclass(AAC::class)
        subclass(AAD::class)
      }
      polymorphic(Base::class) {
        subclass(AA::class)
        subclass(BB::class)
      }
    }
    val demoJson = Json {
      this.namingStrategy
      serializersModule = demoModule
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
    val a1: AA = AAD("hi~")
    println(demoJson.encodeToString(a1))
    val a2: Base = AAD("hi~")
    println(demoJson.encodeToString(a2))
  }

  @Test
  fun testLifecycle() = runCommonTest {
    val endpointLifecycle: EndpointMessage = EndpointLifecycle(EndpointLifecycleInit)
    assertEquals(
      """{"type":"life","state":{"name":"init"},"order":-1}""",
      endpointMessageToJson(endpointLifecycle),
    )


    val endpointIpcMessage: EndpointMessage = EndpointIpcMessage(
      11, IpcLifecycle(
        IpcLifecycleInit(22,
          CommonAppManifest().also { it.id = "A" },
          CommonAppManifest().also { it.id = "B" })
      )
    )
    assertEquals(
      """{"type":"ipc","pid":11,"ipcMessage":{"type":"life","state":{"name":"init","pid":22,"locale":{"id":"A","dweb_deeplinks":[],"dweb_protocols":[],"dweb_permissions":[],"name":"","short_name":"","icons":[],"categories":[],"shortcuts":[],"version":"0.0.1"},"remote":{"id":"B","dweb_deeplinks":[],"dweb_protocols":[],"dweb_permissions":[],"name":"","short_name":"","icons":[],"categories":[],"shortcuts":[],"version":"0.0.1"}},"order":-1}}""",
      endpointMessageToJson(endpointIpcMessage),
    )
  }

  @Test
  fun testEvent() = runCommonTest {

    val endpointIpcMessage: EndpointMessage = EndpointIpcMessage(
      11, IpcEvent.fromUtf8("hi", "QWQ")
    )
    assertEquals(
      """{"type":"ipc","pid":11,"ipcMessage":{"type":"event","name":"hi","data":"QWQ","encoding":2,"order":null}}""",
      endpointMessageToJson(endpointIpcMessage),
    )
  }

}