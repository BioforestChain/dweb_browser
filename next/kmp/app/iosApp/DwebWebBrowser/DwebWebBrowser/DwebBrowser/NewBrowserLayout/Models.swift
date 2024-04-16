//
//  Models.swift
//  SwiftUITest
//
//  Created by instinct on 2024/4/7.
//

import Foundation
import Observation
import SwiftUI

enum DwebUrl {
    static let Blank = "blank"
    static let History = "History"
    static let Privacy = "privacy"
    
    case blank
    case privacy
    case history
    case url(String, Int)
    
    var id: Int {
        var hasher = Hasher()
        switch self {
            case .blank:
                hasher.combine(DwebUrl.Blank)
            case .privacy:
                hasher.combine(DwebUrl.Privacy)
            case .history:
                hasher.combine(DwebUrl.History)
            case .url(let urlStr, let salt):
                hasher.combine(urlStr)
                hasher.combine(salt)
        }
        return hasher.finalize()
    }
    
    static func URL(_ urlStr: String) -> DwebUrl {
        return DwebUrl.url(urlStr, Int(Date().timeIntervalSince1970))
    }
}

extension String {
    func toDwebUrl() -> DwebUrl {
        switch self {
            case DwebUrl.Blank: DwebUrl.blank
            case DwebUrl.Privacy: DwebUrl.privacy
            case DwebUrl.History: DwebUrl.history
            default: DwebUrl.URL(self)
        }
    }
}

  
@Observable class PageModel: Identifiable {

    // Data or State
    var url: DwebUrl = .blank
    var fav: Image? = nil
    var progress: Float = 0.0 {
        didSet {
            address.progress = progress
        }
    }
    
    var address = AddressModel.empty
    
    @ObservationIgnored
    let id: Int {
        url.id
    }
        
    init(url: DwebUrl = .blank) {
        self.url = url
        addressBindWebPageState()
    }
    
    func addressBindWebPageState() {
        address.rightButtonAction = { [weak self] text in
            self?.load(text)
        }
    }
    
    // Action
    func load(_ text: String) {
        url = text.toDwebUrl()
    }
    
    func back() {
        print("back")
    }
}

class WebModel {
    let index: String
    init(index: String) {
        self.index = index
    }
}

class AddressModel {
    
    class var empty: AddressModel {
        AddressModel(leftImage: .systerm("magnifyingglass"),
                     rightImage: .systerm("qrcode.viewfinder"),
                     text: "https:www.baidu.com") { text in
            
        }
    }
    
    let leftImage: DwebImage
    let rightImage: DwebImage
    var text: String
    var rightButtonAction: (String)->Void
    var progress: Float = 0.0  {
        didSet {
            if progress < 0.0 {
                progress = 0.0
            } else if progress > 1.0 {
                progress = 1.0
            }
        }
    }
    
    var textBinding: Binding<String> {
        Binding { [weak self] in
            self?.text ?? ""
        } set: { [weak self] t in
            self?.text = t
        }
    }
    
    init(leftImage: DwebImage, rightImage: DwebImage, text: String, action: @escaping (String)->Void) {
        self.leftImage = leftImage
        self.rightImage = rightImage
        self.text = text
        self.rightButtonAction = action
    }
}

enum ContainerMoreItem: Identifiable {
    var id: String {
        return title
    }

    case bookmark((ContainerMoreItem)->Void)
    case tackless(Bool, (ContainerMoreItem)->Void)
    case privacy((ContainerMoreItem)->Void)
    case scan((ContainerMoreItem)->Void)
    case download((ContainerMoreItem)->Void)
    case history((ContainerMoreItem)->Void)

    var icon: String {
        switch self {
        case .bookmark:
            "bookmark"
        case let .tackless(trackless, _):
            trackless ? "lock.icloud.fill" : "lock.icloud"
        case .privacy:
            "shield.lefthalf.filled"
        case .scan:
            "qrcode.viewfinder"
        case .download:
            "square.and.arrow.down"
        case .history:
            "clock"
        }
    }

    var title: String {
        switch self {
        case .bookmark:
            "Bookmark"
        case let .tackless(trackless, _):
            trackless ? "No Traces" : "Traces"
        case .privacy:
            "Privacy Policy"
        case .scan:
            "Scan"
        case .download:
            "Downloads"
        case .history:
            "History Record"
        }
    }
    
    func doAction() {
        switch self {
        case .bookmark(let action):
            action(self)
        case .tackless(_, let action):
            action(self)
        case .privacy(let action):
            action(self)
        case .scan(let action):
            action(self)
        case .download(let action):
            action(self)
        case .history(let action):
            action(self)
        default:
            print("[iOS] Dweb ===Unknow type===")
        }
    }
}
