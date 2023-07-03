//
//  APPModel.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/5.
//

import Foundation

struct APPModel: Codable {
    var id: String
    var server: [String: String]
    var dweb_deeplinks: [String]
    var icon: String
    var name: String
    var short_name: String
    var description: String
    var images: [String]?
    var bundle_url: String
    var author: [String]?
    var version: String
    var categories: [String]?
    var new_feature: [String]?
    var bundle_size: Int
    var home: String
    var bundle_hash: String
    var plugins: [String]?
    var release_date: String
    var permissions: [String]?

    init(id: String, server: [String: String], dweb_deeplinks: [String], icon: String, name: String, short_name: String, description: String, images: [String]? = nil, bundle_url: String, author: [String]? = nil, version: String, categories: [String]? = nil, new_feature: [String]? = nil, bundle_size: Int, home: String, bundle_hash: String, plugins: [String]? = nil, release_date: String, permissions: [String]? = nil) {
        self.id = id
        self.server = server
        self.dweb_deeplinks = dweb_deeplinks
        self.icon = icon
        self.name = name
        self.short_name = short_name
        self.description = description
        self.images = images
        self.bundle_url = bundle_url
        self.author = author
        self.version = version
        self.categories = categories
        self.new_feature = new_feature
        self.bundle_size = bundle_size
        self.home = home
        self.bundle_hash = bundle_hash
        self.plugins = plugins
        self.release_date = release_date
        self.permissions = permissions
    }

    enum CodingKeys: String, CodingKey {
        case id
        case server
        case dweb_deeplinks
        case icon
        case name
        case short_name
        case description
        case images
        case bundle_url
        case author
        case version
        case categories
        case new_feature
        case bundle_size
        case home
        case bundle_hash
        case plugins
        case release_date
        case permissions
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(server, forKey: .server)
        try container.encode(dweb_deeplinks, forKey: .dweb_deeplinks)
        try container.encode(icon, forKey: .icon)
        try container.encode(name, forKey: .name)
        try container.encode(short_name, forKey: .short_name)
        try container.encode(description, forKey: .description)
        try container.encode(images, forKey: .images)
        try container.encode(bundle_url, forKey: .bundle_url)
        try container.encode(author, forKey: .author)
        try container.encode(version, forKey: .version)
        try container.encode(categories, forKey: .categories)
        try container.encode(new_feature, forKey: .new_feature)
        try container.encode(bundle_size, forKey: .bundle_size)
        try container.encode(home, forKey: .home)
        try container.encode(bundle_hash, forKey: .bundle_hash)
        try container.encode(plugins, forKey: .plugins)
        try container.encode(release_date, forKey: .release_date)
        try container.encode(permissions, forKey: .permissions)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try? container.decode(String.self, forKey: .id)
        let server = try? container.decode([String: String].self, forKey: .server)
        let dweb_deeplinks = try? container.decode([String].self, forKey: .dweb_deeplinks)
        let icon = try? container.decode(String.self, forKey: .icon)
        let name = try? container.decode(String.self, forKey: .name)
        let short_name = try? container.decode(String.self, forKey: .short_name)
        let description = try? container.decode(String.self, forKey: .description)
        let images = try? container.decode([String].self, forKey: .images)
        let bundle_url = try? container.decode(String.self, forKey: .bundle_url)
        let author = try? container.decode([String].self, forKey: .author)
        let version = try? container.decode(String.self, forKey: .version)
        let categories = try? container.decode([String].self, forKey: .categories)
        let new_feature = try? container.decode([String].self, forKey: .new_feature)
        let bundle_size = try? container.decode(Int.self, forKey: .bundle_size)
        let home = try? container.decode(String.self, forKey: .home)
        let bundle_hash = try? container.decode(String.self, forKey: .bundle_hash)
        let plugins = try? container.decode([String].self, forKey: .plugins)
        let release_date = try? container.decode(String.self, forKey: .release_date)
        let permissions = try? container.decode([String].self, forKey: .permissions)
        self.init(id: id ?? "", server: server ?? [:], dweb_deeplinks: dweb_deeplinks ?? [], icon: icon ?? "", name: name ?? "", short_name: short_name ?? "", description: description ?? "", images: images, bundle_url: bundle_url ?? "", author: author, version: version ?? "", categories: categories, new_feature: new_feature, bundle_size: bundle_size ?? 0, home: home ?? "", bundle_hash: bundle_hash ?? "", plugins: plugins, release_date: release_date ?? "", permissions: permissions)
    }
}

// 下载状态
enum DownloadStatus: Int {
    case IDLE
    case Downloading
    case DownloadComplete
    case Pause
    case Installed
    case Fail
    case Cancel
    case NewVersion
}
