package org.dweb_browser.core.help.types

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.PropMetas
import org.dweb_browser.helper.PropMetasSerializer
import org.dweb_browser.helper.ShortcutItem

typealias MMID = String
typealias DWEB_DEEPLINK = String
typealias DWEB_PROTOCOL = MMID
/**
 * MMID or Protocol
 * 这里的定义，目的是在代码中明确区分出需求
 */
typealias MMPT = MMID

object CommonAppManifestSerializer : PropMetasSerializer<CommonAppManifest>(CommonAppManifest.P)

@Serializable(with = CommonAppManifestSerializer::class)
class CommonAppManifest(p: PropMetas.PropValues = P.buildValues()) :
  PropMetas.Constructor<CommonAppManifest>(p, P), ICommonAppManifest {
  companion object {
    internal val P = PropMetas("CommonAppManifest") { CommonAppManifest(it) }
    private val P_id = P.required("id", "")
    private val P_dweb_deeplinks = P.list<DWEB_DEEPLINK>("dweb_deeplinks")
    private val P_dweb_protocols = P.list<DWEB_PROTOCOL>("dweb_protocols")
    private val P_dweb_permissions = P.list<DwebPermission>("dweb_permissions")
    private val P_dir = P.optional<String>("dir")
    private val P_lang = P.optional<String>("lang")
    private val P_name = P.required("name", "")
    private val P_short_name = P.required("short_name", "")
    private val P_description = P.optional<String>("description")
    private val P_icons = P.list<ImageResource>("icons")
    private val P_homepage_url = P.optional<String>("homepage_url")
    private val P_screenshots = P.mutableListOptional<ImageResource>("screenshots")
    private val P_display = P.optional<DisplayMode>("display")
    private val P_orientation = P.optional<String>("orientation")
    private val P_categories = P.list<MICRO_MODULE_CATEGORY>("categories")
    private val P_theme_color = P.optional<String>("theme_color")
    private val P_background_color = P.optional<String>("background_color")
    private val P_shortcuts = P.list<ShortcutItem>("shortcuts")
    private val P_version = P.required("version", "0.0.1")
  }

  override var id by P_id(p)
  override var dweb_deeplinks by P_dweb_deeplinks(p)
  override var dweb_protocols by P_dweb_protocols(p)
  override var dweb_permissions by P_dweb_permissions(p)
  override var dir by P_dir(p)
  override var lang by P_lang(p)
  override var name by P_name(p)
  override var short_name by P_short_name(p)
  override var description by P_description(p)
  override var homepage_url by P_homepage_url(p)
  override var icons by P_icons(p)
  override var screenshots by P_screenshots(p)
  override var display by P_display(p)
  override var orientation by P_orientation(p)
  override var categories by P_categories(p)
  override var theme_color by P_theme_color(p)
  override var background_color by P_background_color(p)
  override var shortcuts by P_shortcuts(p)
  override var version by P_version(p)

  override fun toString(): String {
    return "CommonAppManifest@${hashCode()}"
  }
}

interface ICommonAppManifest {
  var id: MMID
  var dweb_deeplinks: List<DWEB_DEEPLINK>
  var dweb_protocols: List<DWEB_PROTOCOL>
  var dweb_permissions: List<DwebPermission>
  var dir: String?// 文本方向
  var lang: String?
  var name: String// 应用名称
  var short_name: String // 应用副标题
  var description: String?
  var homepage_url: String?
  var icons: List<ImageResource>
  var screenshots: List<ImageResource>?
  var display: DisplayMode?
  var orientation: String?
  var categories: List<MICRO_MODULE_CATEGORY>// 应用类型
  var theme_color: String?
  var background_color: String?
  var shortcuts: List<ShortcutItem>
  var version: String
}