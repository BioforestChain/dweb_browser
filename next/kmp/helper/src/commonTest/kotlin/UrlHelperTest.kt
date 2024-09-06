package info.bagen.dwebbrowser

import org.dweb_browser.helper.isMaybeDomain
import org.dweb_browser.helper.parseAsDomain
import org.dweb_browser.helper.toIpcUrl
import kotlin.test.Test

class UrlHelperTest {
  @Test
  fun fileUrlParametersTest() {
    val urlString = "file://http.std.dweb/listen?token=1234&domain=abc"

    println(urlString.toIpcUrl())
    println(urlString.toIpcUrl().toString())
    println(urlString.toIpcUrl().parameters)
  }

  @Test
  fun test1() {
    println(true || true && true)   // true
    println(true || true && false)  // true
    println(true || false && true)  // true
    println(true || false && false) // true
    println(false || true && true)  // true
    println(false || true && false) // false
    println(false || false && true) // false
    println(false || false && false)// false
  }

  @Test
  fun hostTest() {
    "baidu".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "www.baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "http://baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "http://www.baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "-www.baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "www.baidu.com-".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }
    "www.baidu.com/".let { println("${it.padEnd(36, ' ')} = ${it.isMaybeDomain()}") }


    "baidu".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
    "baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
    "www.baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
    "http://baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
    "http://www.baidu.com".let {
      println(
        "${
          it.padEnd(
            36,
            ' '
          )
        } = ${it.parseAsDomain().errorMessage}"
      )
    }
    "-www.baidu.com".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
    "www.baidu.com-".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
    "www.baidu.com/".let { println("${it.padEnd(36, ' ')} = ${it.parseAsDomain().errorMessage}") }
  }
}