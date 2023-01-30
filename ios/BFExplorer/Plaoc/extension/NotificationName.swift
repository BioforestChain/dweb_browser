//
//  NotificationName.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/11.
//

import Foundation

extension Notification.Name {
    
    static let interceptNotification = Notification.Name(rawValue: "intercept.request")
    static let progressNotification = Notification.Name(rawValue: "download.progress")
    static let openDwebNotification = Notification.Name(rawValue: "com.openDweb")
    static let closeAnAppNotification = Notification.Name(rawValue: "closeAnAppNotification")
}
