package info.bagen.dwebbrowser

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.Query
import kotlin.test.Test

class QueryTest {
  @Test
  fun queryStringTest() {

    @Serializable
    data class Q(
      val a: String,
      val b: Boolean,
      val c: List<Float>,
      val d: List<Int>,
      val e: List<String>,
    )

    val queryString =
      "a=text&b=true&c=1.1&c=2.2&d=1&d=2&e=either&e=eight&f={\"a\"=\"aaa\", \"b\"=false}"
    val q = Query.decodeFromSearch<Q>(queryString)
    println("$q")
  }
}