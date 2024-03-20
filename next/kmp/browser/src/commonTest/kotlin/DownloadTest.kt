import io.ktor.http.Url
import org.dweb_browser.helper.padEndAndSub
import kotlin.test.Test

class DownloadTest {

  @Test
  fun has265Test() {
  }

  @Test
  fun urlTest() {
    "baidu".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "123".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    ".".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "http://baidu".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "http://baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "http://www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "file://abc".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "file://www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "abc://www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "abc:www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "abc:/www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
    "abc//www.baidu.com".apply { println("${this.padEndAndSub(32)} => ${Url(this)}") }
  }
}