package org.dweb_browser.browser.search

import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleSignal

class SearchController(searchNMM: SearchNMM) {
  private val searchStore = SearchStore(searchNMM)

  val searchEngineList = mutableListOf<SearchEngine>()
  val searchInjectList = mutableListOf<SearchInject>()

  // 状态更新信号
  internal val engineUpdateSignal = SimpleSignal()
  val onEngineUpdate = engineUpdateSignal.toListener()

  init {
    searchNMM.ioAsyncScope.launch {
      searchEngineList.addAll(searchStore.getAllEnginesState())
      engineUpdateSignal.emit()
      searchInjectList.addAll(searchStore.getAllInjects())
    }
  }

  /**
   * 判断当前的关键字是否是引擎，如果是，可以打开搜索界面，并且返回引擎的主页地址
   */
  suspend fun checkAndEnableEngine(key: String): String? {
    val current = searchEngineList.firstOrNull { it.keys.split(",").contains(key) }
    return current?.let {
      current.enable = true
      searchStore.saveEngineState(current)
      engineUpdateSignal.emit()
      current.homeLink
    }
  }

  suspend fun inject(searchInject: SearchInject): Boolean {
    searchInjectList.add(searchInject) // TODO 暂时没做去重等操作。
    searchStore.saveInject(searchInjectList)
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