//
//  PermissionManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/12.
//

import Foundation
import Photos
import AssetsLibrary
import MediaPlayer
import CoreTelephony
import CoreLocation
import AVFoundation
import Contacts
import UserNotifications
import CoreBluetooth

let permissionManager = PermissionManager()
class PermissionManager: NSObject {
    
    enum PermissionsType: String {
        //相机
        case camera = "PERMISSION_CAMERA"
        //相册
        case photo = "PERMISSION_PHOTO"
        //定位
        case location = "PERMISSION_LOCATION"
        //网络
        case network = "PERMISSION_NETWORK"
        //麦克风
        case microphone = "PERMISSION_RECORD_AUDIO"
        //媒体库
        case media = "PERMISSION_MEDIA"
        //通讯录
        case contact = "PERMISSION_CONTACTS"
        //通知
        case notification = "PERMISSION_NOTIFICATION"
        //蓝牙
        case bluetooth = "PERMISSION_BLUETOOTH"
    }
    
    func startPermissionAuthenticate(type: PermissionsType, isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        switch type {
        case .camera:
            openCaptureDeviceServiceWithBlock(isSet: isSet, action: action)
        case .photo:
            openAlbumServiceWithBlock(isSet: isSet, action: action)
        case .location:
            openLocationServiceWithBlock(isSet: isSet, action: action)
        case .network:
            openNetworkServiceWithBlock(isSet: isSet, action: action)
        case .microphone:
            openRecordServiceWithBlock(isSet: isSet, action: action)
        case .media:
            openMediaPlayerServiceWithBlock(isSet: isSet, action: action)
        case .contact:
            openContactServiceWithBlock(isSet: isSet, action: action)
        case .notification:
            openNotificationServiceWithBlock(isSet: isSet, action: action)
        case .bluetooth:
            openBlueBoothServiceWithBlock(isSet: isSet, action: action)
        }
    }

    //开启媒体库服务
    private func openMediaPlayerServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        let authStatus = MPMediaLibrary.authorizationStatus()
        switch authStatus {
        case .notDetermined:
            MPMediaLibrary.requestAuthorization { status in
                DispatchQueue.main.async {
                    self.openMediaPlayerServiceWithBlock(isSet: isSet, action: action)
                }
            }
        case .authorized:
            action(true)
        default:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .media)
            }
        }
    }
    //开启互联网服务
    private func openNetworkServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
       let cellularData = CTCellularData()
        cellularData.cellularDataRestrictionDidUpdateNotifier = { (status) in
            if status == .restrictedStateUnknown || status == .notRestricted {
                action(false)
                if isSet ?? false {
                    self.openURLWithAlertView(type: .network)
                }
            } else {
                action(true)
            }
        }
        
        let state = cellularData.restrictedState
        if state == .restrictedStateUnknown || state == .notRestricted {
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .network)
            }
        } else {
            action(true)
        }
    }

    //开启定位
    private func openLocationServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        guard CLLocationManager.locationServicesEnabled() else {
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .location)
            }
            return
        }
        let status = CLLocationManager.authorizationStatus()
        switch status {
        case .notDetermined:
            CLLocationManager().requestWhenInUseAuthorization()
            CLLocationManager().requestAlwaysAuthorization()
            openLocationServiceWithBlock(isSet: isSet, action: action)
        case .restricted, .denied:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .location)
            }
        case .authorizedAlways, .authorizedWhenInUse, .authorized:
            action(true)
        default:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .location)
            }
        }
    }

    //开启摄像头
    private func openCaptureDeviceServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    action(granted)
                    if !granted, isSet ?? false {
                        self.openURLWithAlertView(type: .camera)
                    }
                }
            }
        case .restricted, .denied:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .camera)
            }
        default:
            action(true)
        }
    }

    //开启相册
    private func openAlbumServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        let authStatus = PHPhotoLibrary.authorizationStatus()
        switch authStatus {
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization { status in
                DispatchQueue.main.async {
                    self.openAlbumServiceWithBlock(isSet: isSet, action: action)
                }
            }
        case .authorized:
            action(true)
        default:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .photo)
            }
        }
    }

    //开启麦克风
    private func openRecordServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        let status = AVAudioSession.sharedInstance().recordPermission
        switch status {
        case .denied:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .microphone)
            }
        case .undetermined:
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                DispatchQueue.main.async {
                    action(granted)
                    if !granted, isSet ?? false {
                        self.openURLWithAlertView(type: .microphone)
                    }
                }
            }
        default:
            action(true)
        }
    }
    
    //开启通讯录
    private func openContactServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        let status = CNContactStore.authorizationStatus(for: .contacts)
        switch status {
        case .authorized:
            action(true)
        case .notDetermined:
            CNContactStore().requestAccess(for: .contacts) { status, error in
                DispatchQueue.main.async {
                    action(status)
                    if !status, isSet ?? false {
                        self.openURLWithAlertView(type: .contact)
                    }
                }
            }
        default:
            action(false)
            if isSet ?? false {
                openURLWithAlertView(type: .contact)
            }
        }
    }
    
    //开启通知
    private func openNotificationServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            
            switch settings.authorizationStatus {
            case .authorized:
                action(true)
            case .notDetermined:
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert,.sound,.badge]) { granted, error in
                    action(granted)
                    if !granted, isSet ?? false {
                        self.openURLWithAlertView(type: .notification)
                    }
                }
            case .denied:
                action(false)
                if isSet ?? false {
                    self.openURLWithAlertView(type: .notification)
                }
            default:
                break
            }
        }
    }
    
    //开启蓝牙
    private func openBlueBoothServiceWithBlock(isSet: Bool? = nil, action: @escaping ((Bool) -> Void)) {
        
        let status = CBPeripheralManager.authorization
        switch status {
        case .allowedAlways:
            action(true)
        default:
            action(false)
            if isSet ?? false {
                self.openURLWithAlertView(type: .notification)
            }
        }
    }

    private func openURLWithAlertView(type: PermissionsType? = nil) {
        let title: String = "访问受限"
        var message: String = "请点击“前往”，允许访问权限"
        guard let appName = Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String else { return }
        switch type {
        case .camera:
            message = "请在iPhone的\"设置-隐私-相机\"选项中，允许\"\(appName)\"访问你的相机"
        case .photo:
            message = "请在iPhone的\"设置-隐私-照片\"选项中，允许\"\(appName)\"访问您的相册"
        case .location:
            message = "请在iPhone的\"设置-隐私-定位服务\"选项中，允许\"\(appName)\"访问您的位置，获得更多商品信息"
        case .network:
            message = "请在iPhone的\"设置-蜂窝移动网络\"选项中，允许\"\(appName)\"访问您的移动网络"
        case .microphone:
            message = "请在iPhone的\"设置-隐私-麦克风\"选项中，允许\"\(appName)\"访问您的麦克风"
        case .media:
            message = "请在iPhone的\"设置-隐私-媒体与Apple Music\"选项中，允许\"\(appName)\"访问您的媒体库"
        case .contact:
            message = "请在iPhone的\"设置-隐私-通讯录\"选项中，允许\"\(appName)\"访问您的通讯录"
        case .notification:
            message = "请在iPhone的\"设置-隐私-通知\"选项中，允许\"\(appName)\"访问您的通知"
        default:
            break
        }
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let settingAction = UIAlertAction(title: "前往", style: .default) { action in
            if UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
            }
        }
        let cancelAction = UIAlertAction(title: "取消", style: .cancel)
        alertController.addAction(cancelAction)
        alertController.addAction(settingAction)
        UIApplication.shared.keyWindow?.rootViewController?.present(alertController, animated: true, completion: nil)
        
    }
}


