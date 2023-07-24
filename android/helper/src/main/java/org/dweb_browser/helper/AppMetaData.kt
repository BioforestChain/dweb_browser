package org.dweb_browser.helper

import java.io.Serializable

typealias Mmid = String
typealias DWEB_DEEPLINK = String

data class AppMetaData(
  val id: Mmid, // jmmApp的id
  val server: MainServer = MainServer("/sys", "/server/plaoc.server.js"), // 打开应用地址
  val dweb_deeplinks: MutableList<DWEB_DEEPLINK> = mutableListOf(),
  val name: String = "", // 应用名称
  val short_name: String = "", // 应用副标题
  val icon: String = "", // 应用图标
  val images: List<String>? = null, // 应用截图
  val description: String = "", // 应用描述
  val author: List<String>? = null, // 开发者，作者
  val categories: List<String>? = null, // 应用类型 https://github.com/w3c/manifest/wiki/Categories
  val version: String = "", // 应用版本
  val new_feature: String? = null, // 新特性，新功能
  val home: String = "", // 首页地址
  var bundle_url: String = "", // 下载应用地址
  val bundle_size: String = "", // 应用大小
  val bundle_hash: String = "", // 文件hash
  val permissions: List<String>? = null, // app使用权限的情况
  val plugins: List<String>? = null, // app使用插件的情况
  val release_date: String = "", // 发布时间
  val isRunning: Boolean = false, // 是否正在运行
  val isExpand: Boolean = false, // 是否默认展开窗口
) : Serializable {
  data class MainServer(
    /**
     * 应用文件夹的目录
     */
    val root: String,
    /**
     * 入口文件
     */
    val entry: String
  ) : Serializable
}