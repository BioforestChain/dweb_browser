package org.dweb_browser.browser.search

import org.dweb_browser.helper.SimpleSignal

class SearchController(private val searchNMM: SearchNMM.SearchRuntime) {
  private val searchStore = SearchStore(searchNMM)

  internal val searchEngineList = mutableListOf<SearchEngine>()
  internal val searchInjectList = mutableListOf<SearchInject>()

  // 状态更新信号
  internal val engineUpdateSignal = SimpleSignal()
  val onEngineUpdate = engineUpdateSignal.toListener()

  internal val injectUpdateSignal = SimpleSignal()
  val onInjectUpdate = injectUpdateSignal.toListener()

  init {
    searchNMM.scopeLaunch(cancelable = true) {
      searchEngineList.addAll(searchStore.getAllEnginesState())
      engineUpdateSignal.emit()
      searchInjectList.addAll(searchStore.getAllInjects())
      injectUpdateSignal.emit()
    }
  }

  /**
   * 判断当前的关键字是否是引擎，如果是，可以打开搜索界面，并且返回引擎的主页地址
   */
  suspend fun enableAndGetEngineHomeLink(key: String): String? {
    for (item in searchEngineList) {
      if (item.matchKeyWord(key)) {
        item.enable = true
        engineUpdateSignal.emit()
        searchNMM.scopeLaunch(cancelable = true) { searchStore.saveEngineState(item) }
        return item.homeLink
      }
    }
    return null
  }

  /**
   * 注入内部可搜索数据
   */
  suspend fun inject(searchInject: SearchInject): Boolean {
    searchInjectList.add(searchInject) // TODO 暂时没做去重等操作。
    injectUpdateSignal.emit()
    searchNMM.scopeLaunch(cancelable = true) { searchStore.saveInject(searchInjectList) } // 通知监听
    return true
  }

  /**
   * 搜索符合关键字信息都离线搜索，TODO 具体规则待定
   */
  suspend fun containsInject(key: String): List<SearchInject> {
    return searchInjectList.filter { item ->
      item.name.contains(key)
    }
  }
}