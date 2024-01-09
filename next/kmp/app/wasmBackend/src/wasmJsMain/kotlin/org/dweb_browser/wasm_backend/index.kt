@file:OptIn(ExperimentalJsExport::class)

package org.dweb_browser.wasm_backend

fun main() {
  println("hi wasm backend !!QAQ")
}

@JsExport
fun echo(word: String): String {
  return "echo: $word"
}