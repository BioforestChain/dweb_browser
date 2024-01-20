package org.dweb_browser.pure.image


internal fun List<Pair<String, String>>.removeOriginAndAcceptEncoding() = filter { (key) ->
  /// 把访问源头过滤掉，未来甚至可能需要额外加上，避免同源限制，但具体如何去加，跟对方的服务器有关系，很难有标准答案，所以这里索性直接移除了
  !(key == "Referer" || key == "Origin" || key == "Host" ||
      // 把编码头去掉，用ktor自己的编码头
      key == "Accept-Encoding")
}

internal fun List<Pair<String, String>>.removeCorsAndContentEncoding() = filter { (key) ->
  // 这里过滤掉 访问控制相关的配置，重写成全部开放的模式
  !(key.startsWith("Access-Control-Allow-") ||
      // 跟内容编码与长度有关的，也全部关掉，proxyResponse.bodyAsChannel 的时候，得到的其实是解码过的内容，所以这些内容编码与长度的信息已经不可用了
      key == "Content-Encoding" || key == "Content-Length")
}

internal fun List<Pair<String, String>>.forceCors() = plus(
  arrayOf(
    ("Access-Control-Allow-Credentials" to "true"),
    ("Access-Control-Allow-Origin" to "*"),
    ("Access-Control-Allow-Headers" to "*"),
    ("Access-Control-Allow-Methods" to "*"),
  )
)