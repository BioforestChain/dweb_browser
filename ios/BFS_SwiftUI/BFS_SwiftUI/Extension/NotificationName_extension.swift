//
//  NotificationName_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/4.
//

import UIKit


extension Notification.Name {
    
    static let loadUrl = Notification.Name("loadUrl")
    static let progress = Notification.Name("progress")
    static let goBack = Notification.Name("goBack")
    static let goForward = Notification.Name("goForward")
    static let webViewTitle = Notification.Name("webViewTitle")
    static let hiddenBottomView = Notification.Name("hiddenBottomView")
    static let downloadComplete = Notification.Name(rawValue: "downloadComplete")
    static let downloadFail = Notification.Name(rawValue: "downloadFail")
    static let DownloadAppFinishedNotification = NSNotification.Name("DownloadAppFinishedNotification")
    static let UpdateAppFinishedNotification = NSNotification.Name("UpdateAppFinishedNotification")
}

