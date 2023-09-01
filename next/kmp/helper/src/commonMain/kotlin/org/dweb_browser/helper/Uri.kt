package org.dweb_browser.helper

data class Uri(val scheme: String, val userInfo: String, val host: String, val port: Int?, val path: String, val query: String, val fragment: String) : Comparable<Uri> {

  companion object {
    private val AUTHORITY = Regex("(?:([^@]+)@)?([^:]+)(?::([\\d]+))?")
    private val RFC3986 = Regex("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?")

    fun of(value: String): Uri {
      val result = RFC3986.matchEntire(value) ?: throw RuntimeException("Invalid Uri: $value")
      val (scheme, authority, path, query, fragment) = result.destructured
      val (userInfo, host, port) = parseAuthority(authority)
      return Uri(scheme, userInfo, host, port, path, query, fragment)
    }

    private fun parseAuthority(authority: String): Triple<String, String, Int?> = when {
      authority.isBlank() -> Triple("", "", null)
      else -> {
        val (userInfo, host, portString) = AUTHORITY.matchEntire(authority)?.destructured
          ?: throw RuntimeException("Invalid authority: $authority")
        val port = portString.toIntOrNull()
        Triple(userInfo, host, port)
      }
    }
  }

  val authority = StringBuilder()
    .appendIfNotBlank(userInfo, userInfo, "@")
    .appendIfNotBlank(host, host)
    .appendIfPresent(port, ":", port.toString())
    .toString()

  fun scheme(scheme: String) = copy(scheme = scheme)
  fun userInfo(userInfo: String) = copy(userInfo = userInfo)
  fun host(host: String) = copy(host = host)
  fun port(port: Int?) = copy(port = port)
  fun path(path: String) = copy(path = path)
  fun query(query: String) = copy(query = query)
  fun fragment(fragment: String) = copy(fragment = fragment)

  fun authority(authority: String): Uri = parseAuthority(authority).let { (userInfo, host, port) ->
    copy(userInfo = userInfo, host = host, port = port)
  }

  fun resolve(relativeUri: String): Uri {
    val relative = of(relativeUri)

    // 如果相对URI已经是绝对的,直接返回
    if (relative.scheme.isNotBlank()) {
      return relative
    }

    // 构建新的URI
    return Uri(
      scheme = scheme,
      host = host,
      port = port,
      userInfo = relative.authority.takeIf { it.isNotBlank() } ?: authority,
      path = if (relative.path.startsWith("/")) {
        relative.path
      } else {
        listOf(path, relative.path).filter { it.isNotBlank() }.joinToString("/")
      },
      query = if (relative.query.isBlank()) {
        query
      } else {
        listOf(query, relative.query).filter { it.isNotBlank() }.joinToString("&")
      },
      fragment = relative.fragment
    )
  }

  override fun compareTo(other: Uri) = toString().compareTo(other.toString())

  override fun toString() = StringBuilder()
    .appendIfNotBlank(scheme, scheme, ":")
    .appendIfNotBlank(authority, "//", authority)
    .append(when {
      authority.isBlank() -> path
      path.isBlank() || path.startsWith("/") -> path
      else -> "/$path"
    })
    .appendIfNotBlank(query, "?", query)
    .appendIfNotBlank(fragment, "#", fragment).toString()
}
fun Uri.removeQuery(name: String) = copy(query = query.toParameters().filterNot { it.first == name }.toUrlFormEncoded())

fun Uri.removeQueries(prefix: String) =
  copy(query = query.toParameters().filterNot { it.first.startsWith(prefix) }.toUrlFormEncoded())

fun Uri.query(name: String, value: String?): Uri =
  copy(query = query.toParameters().plus(name to value).toUrlFormEncoded())

fun Uri.queryParametersEncoded(): Uri =
  copy(query = query.toParameters().toUrlFormEncoded())

/**
 * @see [RFC 3986, appendix A](https://www.ietf.org/rfc/rfc3986.txt)
 */
private val validPathSegmentChars = setOf(
  '~', '-', '.', '_',                                // unreserved
  '!', '$', '&', '\'', '(', ')', '+', ',', ';', '=', // sub-delims
  ':', '@'                                           // valid
)

private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

private fun Char.isValidSpecialPathSegmentChar() = validPathSegmentChars.contains(this)

fun String.toPathSegmentEncoded(): String =
  this.map {
    when {
      it.isAsciiLetter() || it.isDigit() || it.isValidSpecialPathSegmentChar() -> it
      it.isWhitespace() -> "%20"
      else -> it.toString().decodeURI()
    }
  }.joinToString(separator = "")

fun String.toPathSegmentDecoded(): String =
  this.replace("+", "%2B").decodeURI()

fun Uri.extend(uri: Uri): Uri =
  appendToPath(uri.path).copy(query = (query.toParameters() + uri.query.toParameters()).toUrlFormEncoded())

fun Uri.appendToPath(pathToAppend: String?): Uri =
  if (pathToAppend.isNullOrBlank()) this
  else copy(path = (path.removeSuffix("/") + "/" + pathToAppend.removePrefix("/")))

fun StringBuilder.appendIfNotBlank(valueToCheck: String, vararg toAppend: String): StringBuilder =
  appendIf({ valueToCheck.isNotBlank() }, *toAppend)

fun StringBuilder.appendIfNotEmpty(valueToCheck: List<Any>, vararg toAppend: String): StringBuilder =
  appendIf({ valueToCheck.isNotEmpty() }, *toAppend)

fun StringBuilder.appendIfPresent(valueToCheck: Any?, vararg toAppend: String): StringBuilder =
  appendIf({ valueToCheck != null }, *toAppend)

fun StringBuilder.appendIf(condition: () -> Boolean, vararg toAppend: String): StringBuilder = apply {
  if (condition()) toAppend.forEach { append(it) }
}

private fun String.toParameter(): Parameter = split("=", limit = 2).map(String::fromFormEncoded).let { l -> l.elementAt(0) to l.elementAtOrNull(1) }
