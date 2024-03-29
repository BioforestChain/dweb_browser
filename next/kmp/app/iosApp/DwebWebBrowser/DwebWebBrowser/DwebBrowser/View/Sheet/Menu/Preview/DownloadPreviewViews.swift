//
//  DownloadPreviewViews.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/11.
//

import SwiftUI

protocol DownloadPreview: View {
    init(_ url: URL)
    static func isSupport(_ mime: DownloadDataMIME, _ localPath: String?) -> Bool
}

struct DownloadEmptyPreviewView: DownloadPreview {
    let url: URL
    init(_ url: URL) {
        self.url = url
    }
    
    static func isSupport(_ mime: DownloadDataMIME, _ localPath: String?) -> Bool {
        return true
    }
    
    var body: some View {
        Text("Empty:\(url.lastPathComponent)")
    }
}

struct DownloadPreviewDispatch {
    // priority: hight -> low
    static let supportPreviewTypes: [any DownloadPreview.Type] = [
        DownloadImagePreviewView.self,
        DownloadAudioPreviewView.self,
        DownloadVideoPreviewView.self,
        DownloadTextPreviewView.self,
    ]
    
    static func isSupportPreview(_ mime: DownloadDataMIME, _ localPath: String?) -> Bool {
        return supportPreviewTypes.first { type in
            return type.isSupport(mime, localPath)
        } != nil
    }
    
    static func dispathPreview(_ mime: DownloadDataMIME, _ localPath: String?) -> any DownloadPreview {

        guard let filePath = localPath else {
            return DownloadEmptyPreviewView(URL(filePath: "/Error"))
        }

        let type = supportPreviewTypes.first { type in
            return type.isSupport(mime, localPath)
        }
        
        if let type = type {
            return type.init(URL(filePath: filePath))
        } else {
            return DownloadEmptyPreviewView(URL(filePath: "/NoSupportPreviewType"))
        }
    }
}
