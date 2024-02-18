package org.dweb_browser.browser.search

class SearchController(private val searchNMM: SearchNMM) {
  private val browserStore = SearchStore(searchNMM)

  fun isSearchEngine(key: String) = SearchEngineList.firstOrNull {
    it.name.split(",").contains(key)
  } != null

  fun enableEngine(key: String): Boolean {
    val current = SearchEngineList.firstOrNull { it.name.split(",").contains(key) }
    return current?.let { current.enable = true; true } ?: false
  }

  fun engineSearch(key: String): List<SearchEngine>? {
    return null
  }
}