

import js.promise.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.dweb_browser.js_frontend.ViewModel
import kotlinx.coroutines.launch
import react.dom.client.createRoot
import web.dom.document
import react.*
import react.useState
import react.dom.*
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.button
import web.http.fetch
import kotlin.reflect.KProperty


fun main(){
  val viewModel = ViewModel(mutableMapOf<String, dynamic>("currentCount" to 10))
  viewModel.start()
  val container = document.getElementById("root") ?: error("Couldn't find root container!")
  val root = createRoot(container)
  root.render(createApp(viewModel))
}

fun createApp(viewModel: ViewModel): ReactElement<PropsWithChildren>{
  return FC<Props>{ props ->
    var currentState by viewModel.toUseState<Int>("currentCount")

    h1 {
      + "标题1"
    }
    p {
      span{
        + "count:"
      }
      span {
        + "${currentState}"
      }
    }
    button{
      onClick = {
//        viewModel.set("count", 1)
        // TODO: 更新state
        currentState++
      }
      + "increment"
    }
    button{
      onClick = {
        CoroutineScope(Dispatchers.Default).launch{
          val response = fetch("http://127.0.0.1:8888/jsFrontEnd/index.html")
          console.log(response.text().await())
        }
      }
      + "测试请求html"
    }
    button{
      onClick = {
        viewModel.electronWindowOperation.close()
      }
      + "关闭window"
    }
  }.create()
}












//package org.dweb_browser.js_frontend
//
//import kotlinx.coroutines.launch
//import react.dom.client.createRoot
//import react.createElement
//import web.dom.document
//import react.*
//import react.dom.*
//import react.dom.html.ReactHTML.h1
//import react.dom.html.ReactHTML.p
//import react.dom.html.ReactHTML.span
//import react.dom.html.ReactHTML.button
//
//
//
//
////fun main() {
////
////  val viewModel = ViewModel(mutableMapOf<String, dynamic>("currentCount" to 0))
////  viewModel.scope.launch {
////    viewModel.toFlow().collect{
////      console.log("接收到了同步的消息", it)
////    }
////  }
////  viewModel.start()
////  viewModel.scope.launch {
////    viewModel.whenSyncDataFromServerStart.await()
////    viewModel.set("age", 10)
////  }
////
////  // TODO: 使用 React 实现 DOM
////  val container = document.getElementById("root") ?: error("Couldn't find root container!")
////  val root = createRoot(container)
////  root.render(createApp(viewModel))
////}
//
//
////fun createApp(viewModel: ViewModel): ReactElement<PropsWithChildren>{
////  return Fragment.create {
////    val state by useState<MutableMap<dynamic, dynamic>>(viewModel.state)
////    h1 {
////      + "标题"
////    }
////    p {
////      span{
////        + "count:"
////      }
////      span {
////        + "${state["currentCount"]}"
////      }
////    }
////    button{
////      onClick = {
////        viewModel.set("count", 1)
////      }
////      + "increment"
////    }
////
////  }
////}
//
//
//
//import react.*
//import react.dom.html.ReactHTML.h1
//import react.dom.client.createRoot
//import web.dom.document
//
////fun main() {
////  val container = document.getElementById("root") ?: error("Couldn't find root container!")
////  createRoot(container).render(Fragment.create {
////
////    h1 {
////      +"Hello, React+Kotlin/JS!"
////    }
////  })
////}
//
////fun main() {
////    document.bgColor = "red"
////}
//
