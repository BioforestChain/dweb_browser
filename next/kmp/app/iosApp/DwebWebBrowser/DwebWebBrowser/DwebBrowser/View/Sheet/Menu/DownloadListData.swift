//
//  DownloadListData.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/12.
//

import Foundation
import Observation

extension WebBrowserViewDownloadData {
    func toDownloadState() -> DownloadItem.State {
        /// 0: init, 1：下载中，2：暂停，4：取消,  5:失败，6: 完成
        switch status {
            case 1:  DownloadItem.State.loading(progress)
            case 2:  DownloadItem.State.pause(progress)
            case 3:  DownloadItem.State.cancel
            case 4:  DownloadItem.State.fail
            case 5:  DownloadItem.State.loaded(localPath ?? "")
            default: DownloadItem.State.none
        }
    }
    
    var isNeedObserved: Bool {
        switch status {
            case 1, 2, 4: true
            default: false
        }
    }
}


enum DownloadDataMIME {
    case text(String)
    case image(String)
    case audio(String)
    case video(String)
    case application(String) // application
    case other(String)
    
    var sfIcon: String {
        switch self {
            case .text:
                "doc.plaintext"
            case .image:
                "photo"
            case .audio:
                "speaker.wave.3"
            case .video:
                "video"
            case let .application(subType):
                switch subType {
                    case "json": "doc.plaintext"
                    case "xml": "tablecells"
                    case "pdf": "doc.plaintext"
                    case "zip": "doc.zipper"
                    default:
                        "doc"
                }
            default:
                "doc"
        }
    }
}

extension String {
    func toDownloadDataType() -> DownloadDataMIME {
        let compents = components(separatedBy: "/")
        
        guard compents.count == 2 else {
            return DownloadDataMIME.other(self)
        }
        //
        return switch compents.first! {
            case "text": .text(compents.last!)
            case "image": .image(compents.last!)
            case "audio": .audio(compents.last!)
            case "video": .video(compents.last!)
            case "application": .application(compents.last!)
            default:
                .other(self)
        }
    }
}

class DownloadItem: Identifiable, ObservableObject {
    
    enum State {
        case none
        case loading(Float /*进度*/)
        case pause(Float /*进度*/)
        case cancel
        case fail
        case loaded(String /*下载后本地的文件路径*/)
    }
    
    // 只在state == loading, pause有效
    var pause: Bool = false
    
    var isLoaded: Bool {
        switch state {
            case .loaded(_):
                true
            default:
                false
        }
    }
    
    let id: String
    let mime: DownloadDataMIME
    let title: String
    let date: String //时间戳
    let dateValue: UInt64 //时间戳，排序用的。
    let size: String
    let localPath: String?
    
    @Published var state: State
    
        
    var url: URL {
        let p = Bundle.main.path(forResource: "hello", ofType: "txt") ?? ""
        return URL(filePath: p)
    }
        
    init(id: String, mime: DownloadDataMIME, title: String, date: String, dateValue: UInt64, size: String, state: State, localPath: String?) {
        self.id = id
        self.mime = mime
        self.title = title
        self.date = date
        self.dateValue = dateValue
        self.size = size
        self.state = state
        self.localPath = localPath
        updatePause()
    }
    
    func updateState(_ state: State) {
        Log("\(title) update: \(state)")
        self.state = state
        self.updatePause()
    }
    
    private func updatePause() {
        if case .pause(_) = state {
            pause = true
        } else {
            pause = false
        }
    }
}
