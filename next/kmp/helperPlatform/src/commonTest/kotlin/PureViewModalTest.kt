@file:Suppress("UNSUPPORTED_FEATURE")

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.dweb_browser.helper.platform.StateContext
import org.dweb_browser.helper.platform.mutableStateFlow
import org.dweb_browser.test.runCommonTest
import kotlin.reflect.KClass
import kotlin.test.Test

@OptIn(InternalSerializationApi::class)
class TestStateContext : StateContext {
  override fun <I : Any> emitChange(iKClass: KClass<I>, input: I) {
    Json.encodeToString(iKClass.serializer(), input).also {
      println("TEST emitChange $it")
    }
  }
}

class Data1(ctx: StateContext) {
  val a = ctx.mutableStateFlow(1)  // id=1//.collectAsMutableState()
  val b = ctx.mutableStateFlow("xx") // id=2
//  val c = with(ctx.mutableStateListFlow(listOf<Int>())) { // id=3
//    emitChange("add" to item to index)
//  }
//  val obj = ctx.mutableStateObjectFlow { subctx -> // id=4
//    object : {
//      val cc = subctx.mutableStateFlow(listOf<Int>())
//    }
//  }
}

class PureViewModalTest {
  @Test
  fun test1() = runCommonTest {
    val jsonCtx = TestStateContext()
    with(jsonCtx) {
      val a = mutableStateFlow(1)
      a.value = 789
    }
    val data1 = Data1(jsonCtx)
    data1.a.value = 123
    data1.b.value = "tititi"
  }
}