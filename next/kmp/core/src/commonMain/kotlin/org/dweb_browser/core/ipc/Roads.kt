package org.dweb_browser.core.ipc

/**当前所有的路,这是全局的*/
val allRoads: RoadsMap = RoadsMap()

/**初始化路和各个端点*/
//fun initRoad() {
//  allRoads.set("worker<->kotlin", WorkerToKotlinRoad())
//  allRoads.set("web<->kotlin", WebToKotlinToRoad())
//}

/**路，每条路都是双向的，会注册到路由表上，路的尽头是endpoint(端点),消息走到尽头需要跟端点问，接下来往哪里走*/
abstract class Roads {
  abstract suspend fun send()
  abstract suspend fun receive()
}

//class WorkerToKotlinRoad : Roads() {
//  override suspend fun send() {
//    TODO("Not yet implemented")
//  }
//
//  override suspend fun receive() {
//    TODO("Not yet implemented")
//  }
//
//}
//
//class WebToKotlinToRoad : Roads() {
//  override suspend fun send() {
//    TODO("Not yet implemented")
//  }
//
//  override suspend fun receive() {
//    TODO("Not yet implemented")
//  }
//}

/***/
class RoadsMap : MutableMap<String, Roads> by LinkedHashMap() {

  fun set(key: String, value: Roads) {
    this[key] = value
  }

  /**接收消息*/
  fun receive(aliasName: String): Roads? {
    return this[aliasName]
  }
}



