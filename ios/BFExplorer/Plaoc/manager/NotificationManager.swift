//
//  NotificationManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/13.
//

import UIKit
import UserNotifications

let notiManager = NotificationManager()

class NotificationManager: NSObject {

    enum NotificationCategory: String {
        case news
    }
    
    enum NotificationCategoryAction: String {
        case like
        case cancel
        case comment
    }
    
    //发送本地通知
    func sendLocalNotification() {
        
        let content = UNMutableNotificationContent()
        content.title = "测试本地通知"
        content.subtitle = "自标题"
        content.body = "提高认知，方有可为"
        content.sound = .default
        content.userInfo = ["info":"haha"]
        content.categoryIdentifier = NotificationCategory.news.rawValue
        
        if let imageURl = Bundle.main.url(forResource: "angle_nft", withExtension: "png") {
            if let attachment = try? UNNotificationAttachment(identifier: "image", url: imageURl, options: nil) {
                content.attachments = [attachment]
            }
        }
        
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 10, repeats: false)
        let requestIdentifier = "com.local"
        let request = UNNotificationRequest(identifier: requestIdentifier, content: content, trigger: trigger)
        
        UNUserNotificationCenter.current().add(request)
    }
    
    //注册category
    func registerNotificationCategory() {
        let newsCategory: UNNotificationCategory = {
            let inputAction = UNTextInputNotificationAction(identifier: NotificationCategoryAction.comment.rawValue, title: "评论", options: [.foreground], textInputButtonTitle: "发送", textInputPlaceholder: "留下你想说的话...")
            
            let likeAction = UNNotificationAction(identifier: NotificationCategoryAction.like.rawValue, title: "点赞", options: [.foreground])
            
            let cancelAction = UNNotificationAction(identifier: NotificationCategoryAction.cancel.rawValue, title: "取消", options: [.destructive])
            
            return UNNotificationCategory(identifier: NotificationCategory.news.rawValue, actions: [inputAction, likeAction, cancelAction], intentIdentifiers: [], options: [.customDismissAction])
        }()
        UNUserNotificationCenter.current().setNotificationCategories([newsCategory])
    }
    
    private func handleNews(response: UNNotificationResponse) {
        var message: String = ""
        if let actionType = NotificationCategoryAction(rawValue: response.actionIdentifier) {
            switch actionType {
            case .like:
                message = "你点击了点赞按钮"
            case .cancel:
                message = "你点击了取消按钮"
            case .comment:
                message = "你输入了：\((response as! UNTextInputNotificationResponse).userText)"
            }
        } else {
            message = ""
        }
        
        if !message.isEmpty {
            if let vc = UIApplication.shared.keyWindow?.rootViewController {
                let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .cancel))
                vc.present(alert, animated: true)
            }
        }
    }
}

extension NotificationManager: UNUserNotificationCenterDelegate {
    //对通知进行响应
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        
        let categoryIdentifier = response.notification.request.content.categoryIdentifier
        if let category = NotificationCategory(rawValue: categoryIdentifier) {
            switch category {
            case .news:
                handleNews(response: response)
            }
        }
        completionHandler()
    }
}
