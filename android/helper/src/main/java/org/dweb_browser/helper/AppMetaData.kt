package org.dweb_browser.helper

import java.io.Serializable

typealias Mmid = String
typealias DWEB_DEEPLINK = String

data class DesktopAppMetaData(
  val running: Boolean = false,
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
) : Serializable

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
) : Serializable

fun AppMetaData.toDesktopAppMetaData(running: Boolean  = false): DesktopAppMetaData {
  return DesktopAppMetaData(
    running = running,
    id = this.id,
    server = this.server,
    dweb_deeplinks = this.dweb_deeplinks,
    name = this.name,
    short_name = this.short_name,
    icon = this.icon,
    images = this.images,
    description = this.description,
    author = this.author,
    categories = this.categories,
    version = this.version,
    new_feature = this.new_feature,
    home = this.home,
    bundle_url = this.bundle_url,
    bundle_size = this.bundle_size,
    bundle_hash = this.bundle_hash,
    permissions = this.permissions,
    plugins = this.plugins,
    release_date = this.release_date
  )
}

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
