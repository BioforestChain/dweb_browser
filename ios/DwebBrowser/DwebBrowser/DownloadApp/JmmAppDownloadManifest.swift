//
//  JmmAppDownloadManifest.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/5.
//

import Foundation

struct JmmAppDownloadManifest: Codable {
    var id: String
    var version: String
    var categories: [String]
    var server: MainServer
    var baseURI: String?
    var dweb_deeplinks: [String]?
    var dir: TextDirectionType
    var lang: String
    var name: String
    var short_name: String
    var description: String
    var icons: [ImageSource]
    var screenshots: [ImageSource]
    var display: DisplayModeType
    var orientation: OrientationType
    var theme_color: String
    var background_color: String
    var shortcuts: [ShortcutItem]
    var logo: String
    var images: [String]
    var bundle_url: String
    var bundle_hash: String
    var bundle_size: Int64
    var change_log: String
    var author: [String]
    var home: String
    var release_date: String
    var permissions: [String]
    var plugins: [String]
    var languages: [String]?
    var download_status: DownloadStatus
    var download_progress: Double

    init(id: String, version: String, categories: [String], server: MainServer, baseURI: String? = nil, dweb_deeplinks: [String]? = nil, dir: TextDirectionType, lang: String, name: String, short_name: String, description: String, icons: [ImageSource], screenshots: [ImageSource], display: DisplayModeType, orientation: OrientationType, theme_color: String, background_color: String, shortcuts: [ShortcutItem], logo: String, images: [String], bundle_url: String, bundle_hash: String, bundle_size: Int64, change_log: String, author: [String], home: String, release_date: String, permissions: [String], plugins: [String], languages: [String]? = nil, download_status: DownloadStatus, download_progress: Double) {
        self.id = id
        self.version = version
        self.categories = categories
        self.server = server
        self.baseURI = baseURI
        self.dweb_deeplinks = dweb_deeplinks
        self.dir = dir
        self.lang = lang
        self.name = name
        self.short_name = short_name
        self.description = description
        self.icons = icons
        self.screenshots = screenshots
        self.display = display
        self.orientation = orientation
        self.theme_color = theme_color
        self.background_color = background_color
        self.shortcuts = shortcuts
        self.logo = logo
        self.images = images
        self.bundle_url = bundle_url
        self.bundle_hash = bundle_hash
        self.bundle_size = bundle_size
        self.change_log = change_log
        self.author = author
        self.home = home
        self.release_date = release_date
        self.permissions = permissions
        self.plugins = plugins
        self.languages = languages
        self.download_status = download_status
        self.download_progress = download_progress
    }

    enum CodingKeys: String, CodingKey {
        case id
        case version
        case categories
        case server
        case baseURI
        case dweb_deeplinks
        case dir
        case lang
        case name
        case short_name
        case description
        case icons
        case screenshots
        case display
        case orientation
        case theme_color
        case background_color
        case shortcuts
        case logo
        case images
        case bundle_url
        case bundle_hash
        case bundle_size
        case change_log
        case author
        case home
        case release_date
        case permissions
        case plugins
        case languages
        case download_status
        case download_progress
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(version, forKey: .version)
        try container.encode(categories, forKey: .categories)
        try container.encode(server, forKey: .server)
        try container.encode(baseURI, forKey: .baseURI)
        try container.encode(dweb_deeplinks, forKey: .dweb_deeplinks)
        try container.encode(dir, forKey: .dir)
        try container.encode(lang, forKey: .lang)
        try container.encode(name, forKey: .name)
        try container.encode(short_name, forKey: .short_name)
        try container.encode(description, forKey: .description)
        try container.encode(icons, forKey: .icons)
        try container.encode(screenshots, forKey: .screenshots)
        try container.encode(display, forKey: .display)
        try container.encode(orientation, forKey: .orientation)
        try container.encode(theme_color, forKey: .theme_color)
        try container.encode(background_color, forKey: .background_color)
        try container.encode(shortcuts, forKey: .shortcuts)
        try container.encode(logo, forKey: .logo)
        try container.encode(images, forKey: .images)
        try container.encode(bundle_url, forKey: .bundle_url)
        try container.encode(bundle_hash, forKey: .bundle_hash)
        try container.encode(bundle_size, forKey: .bundle_size)
        try container.encode(change_log, forKey: .change_log)
        try container.encode(author, forKey: .author)
        try container.encode(home, forKey: .home)
        try container.encode(release_date, forKey: .release_date)
        try container.encode(permissions, forKey: .permissions)
        try container.encode(plugins, forKey: .plugins)
        try container.encode(languages, forKey: .languages)
        try container.encode(download_status, forKey: .download_status)
        try container.encode(download_progress, forKey: .download_progress)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try? container.decode(String.self, forKey: .id)
        let version = try? container.decode(String.self, forKey: .version)
        let categories = try? container.decode([String].self, forKey: .categories)
        let server = try? container.decode(MainServer.self, forKey: .server)
        let baseURI = try? container.decode(String.self, forKey: .baseURI)
        let dweb_deeplinks = try? container.decode([String].self, forKey: .dweb_deeplinks)
        let dir = try? container.decode(TextDirectionType.self, forKey: .dir)
        let lang = try? container.decode(String.self, forKey: .lang)
        let name = try? container.decode(String.self, forKey: .name)
        let short_name = try? container.decode(String.self, forKey: .short_name)
        let description = try? container.decode(String.self, forKey: .description)
        let icons = try? container.decode([ImageSource].self, forKey: .icons)
        let screenshots = try? container.decode([ImageSource].self, forKey: .screenshots)
        let display = try? container.decode(DisplayModeType.self, forKey: .display)
        let orientation = try? container.decode(OrientationType.self, forKey: .orientation)
        let theme_color = try? container.decode(String.self, forKey: .theme_color)
        let background_color = try? container.decode(String.self, forKey: .background_color)
        let shortcuts = try? container.decode([ShortcutItem].self, forKey: .shortcuts)
        let logo = try? container.decode(String.self, forKey: .logo)
        let images = try? container.decode([String].self, forKey: .images)
        let bundle_url = try? container.decode(String.self, forKey: .bundle_url)
        let bundle_hash = try? container.decode(String.self, forKey: .bundle_hash)
        let bundle_size = try? container.decode(Int64.self, forKey: .bundle_size)
        let change_log = try? container.decode(String.self, forKey: .change_log)
        let author = try? container.decode([String].self, forKey: .author)
        let home = try? container.decode(String.self, forKey: .home)
        let release_date = try? container.decode(String.self, forKey: .release_date)
        let permissions = try? container.decode([String].self, forKey: .permissions)
        let plugins = try? container.decode([String].self, forKey: .plugins)
        let languages = try? container.decode([String].self, forKey: .languages)
        let download_status = try? container.decode(DownloadStatus.self, forKey: .download_status)
        let download_progress = try? container.decode(Double.self, forKey: .download_progress)
        self.init(id: id!, version: version!, categories: categories!, server: server!, baseURI: baseURI ?? "", dweb_deeplinks: dweb_deeplinks ?? [], dir: dir!, lang: lang!, name: name!, short_name: short_name!, description: description!, icons: icons!, screenshots: screenshots!, display: display!, orientation: orientation!, theme_color: theme_color!, background_color: background_color!, shortcuts: shortcuts!, logo: logo!, images: images!, bundle_url: bundle_url!, bundle_hash: bundle_hash!, bundle_size: bundle_size!, change_log: change_log!, author: author!, home: home!, release_date: release_date!, permissions: permissions ?? [], plugins: plugins ?? [], languages: languages ?? [], download_status: download_status ?? DownloadStatus.IDLE, download_progress: download_progress ?? 0.0)
    }
}

// 下载状态
enum DownloadStatus: Int, Codable {
    case IDLE
    case Downloading
    case DownloadComplete
    case Pause
    case Installed
    case Fail
    case Cancel
    case NewVersion
}

struct MainServer: Codable {
    var root: String
    var entry: String

    init(root: String, entry: String) {
        self.root = root
        self.entry = entry
    }

    enum CodingKeys: String, CodingKey {
        case root
        case entry
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(root, forKey: .root)
        try container.encode(entry, forKey: .entry)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let root = try? container.decode(String.self, forKey: .root)
        let entry = try? container.decode(String.self, forKey: .entry)
        self.init(root: root ?? "", entry: entry ?? "")
    }
}

enum TextDirectionType: String, Codable {
    case ltr
    case rtl
    case auto
}

enum DisplayModeType: String, Codable {
    case fullscreen
    case standalone
    case minimalUi = "minimal-ui"
    case browser
}

enum OrientationType: String, Codable {
    case any
    case landscape
    case landscapePrimary = "landscape-primary"
    case landscapeSecondary = "landscape-secondary"
    case natural
    case portrait
    case portraitPrimary = "portrait-primary"
    case portraitSecondary = "portrait-secondary"
}

struct ImageSource: Codable {
    var src: String
    var sizes: String?
    var type: String?
    var purpose: String?
    var platform: String?

    init(src: String, sizes: String? = nil, type: String? = nil, purpose: String? = nil, platform: String? = nil) {
        self.src = src
        self.sizes = sizes
        self.type = type
        self.purpose = purpose
        self.platform = platform
    }

    enum CodingKeys: String, CodingKey {
        case src
        case sizes
        case type
        case purpose
        case platform
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(src, forKey: .src)
        try container.encode(sizes, forKey: .sizes)
        try container.encode(type, forKey: .type)
        try container.encode(purpose, forKey: .purpose)
        try container.encode(platform, forKey: .platform)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let src = try? container.decode(String.self, forKey: .src)
        let sizes = try? container.decode(String.self, forKey: .sizes)
        let type = try? container.decode(String.self, forKey: .type)
        let purpose = try? container.decode(String.self, forKey: .purpose)
        let platform = try? container.decode(String.self, forKey: .platform)
        self.init(src: src ?? "", sizes: sizes ?? "", type: type ?? "", purpose: purpose ?? "", platform: platform ?? "")
    }
}

struct ShortcutItem: Codable {
    var name: String
    var url: String
    var short_name: String?
    var description: String?
    var icons: [ImageSource]?

    init(name: String, url: String, short_name: String? = nil, description: String? = nil, icons: [ImageSource]? = nil) {
        self.name = name
        self.url = url
        self.short_name = short_name
        self.description = description
        self.icons = icons
    }

    enum CodingKeys: String, CodingKey {
        case name
        case url
        case short_name
        case description
        case icons
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(name, forKey: .name)
        try container.encode(url, forKey: .url)
        try container.encode(short_name, forKey: .short_name)
        try container.encode(description, forKey: .description)
        try container.encode(icons, forKey: .icons)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let name = try? container.decode(String.self, forKey: .name)
        let url = try? container.decode(String.self, forKey: .url)
        let short_name = try? container.decode(String.self, forKey: .short_name)
        let description = try? container.decode(String.self, forKey: .description)
        let icons = try? container.decode([ImageSource].self, forKey: .icons)
        self.init(name: name ?? "", url: url ?? "", short_name: short_name ?? "", description: description ?? "", icons: icons ?? [])
    }
}
