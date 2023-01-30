//
//  UIDevice_extension.swift
//  DWebBrowser
//
//  Created by mac on 2022/5/16.
//

import Foundation
import UIKit

extension UIDevice {
    
    var resolution: CGSize {
        let width = UIScreen.main.bounds.width
        let height = UIScreen.main.bounds.height
        let scale = UIScreen.main.scale
        let resSize = CGSize(width: width * scale, height: height * scale)
        return resSize
    }
    
    var ppi: CGFloat {
        return 163 * UIScreen.main.scale
    }
    
    var safeArea: UIEdgeInsets {
        return .zero
    }
    
    //判断是不是刘海屏
    func isiPhoneXScreen() -> Bool {
        guard #available(iOS 11.0, *) else {
            return false
        }
        
        let isX = UIApplication.shared.windows[0].safeAreaInsets.bottom > 0
        return isX
    }
    
    func statusBarHeight() -> CGFloat {
        return UIDevice.current.isiPhoneXScreen() ? 44.0 : 20.0
    }
    
    func tabbarSpaceHeight() -> CGFloat {
        return isiPhoneXScreen() ? 34 : 0
    }
    
    public var deviceType: String? {
        return "iOS"
    }
    
    public var device_model: String? {
        var systemInfo = utsname()
        uname(&systemInfo)
        
        let platform = withUnsafePointer(to: &systemInfo.machine.0) { ptr in
            return String(cString: ptr)
        }
        
        switch platform {
        case "iPhone5,1", "iPhone5,2", "iPhone5,3", "iPhone5,4":
            return "iPhone 5"
        case "iPhone6,1", "iPhone6,2":
            return "iPhone 5s"
        case "iPhone7,1", "iPhone7,2", "iPhone8,1", "iPhone8,2":
            return "iPhone 6"
        case "iPhone8,4", "iPhone12,8", "iPhone14,6":
            return "iPhone SE"
        case "iPhone9,1", "iPhone9,2", "iPhone9,3", "iPhone9,4":
            return "iPhone 7"
        case "iPhone10,1", "iPhone10,4", "iPhone10,2", "iPhone10,5":
            return "iPhone 8"
        case "iPhone10,3", "iPhone10,6":
            return "iPhone X"
        case "iPhone11,8":
            return "iPhone XR"
        case "iPhone11,2":
            return "iPhone XS"
        case "iPhone11,6", "iPhone11,4":
            return "iPhone XSMax"
        case "iPhone12,1":
            return "iPhone 11"
        case "iPhone12,3":
            return "iPhone 11 Pro"
        case "iPhone12,5":
            return "iPhone 11 Pro Max"
        case "iPhone13,1":
            return "iPhone 12 mini"
        case "iPhone13,2":
            return "iPhone 12"
        case "iPhone13,3":
            return "iPhone 12 Pro"
        case "iPhone13,4":
            return "iPhone 12 Pro Max"
        case "iPhone14,4":
            return "iPhone 13 mini"
        case "iPhone14,5":
            return "iPhone 13"
        case "iPhone14,2":
            return "iPhone 13 Pro"
        case "iPhone14,3":
            return "iPhone 13 Pro Max"
        default:
            return platform
        }
    }
    
    public var deviceIP: String {
        var addresses = [String]()
        var ifaddr : UnsafeMutablePointer<ifaddrs>? = nil
        if getifaddrs(&ifaddr) == 0 {
            var ptr = ifaddr
            while (ptr != nil) {
                let flags = Int32(ptr!.pointee.ifa_flags)
                var addr = ptr!.pointee.ifa_addr.pointee
                if (flags & (IFF_UP|IFF_RUNNING|IFF_LOOPBACK)) == (IFF_UP|IFF_RUNNING) {
                    if addr.sa_family == UInt8(AF_INET) || addr.sa_family == UInt8(AF_INET6) {
                        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                        if (getnameinfo(&addr, socklen_t(addr.sa_len), &hostname, socklen_t(hostname.count),nil, socklen_t(0), NI_NUMERICHOST) == 0) {
                            if let address = String(validatingUTF8:hostname) {
                                addresses.append(address)
                            }
                        }
                    }
                }
                ptr = ptr!.pointee.ifa_next
            }
            freeifaddrs(ifaddr)
        }
        return addresses.first ?? ""
    }
    //总内存大小
    public var totalMemorySize: UInt64 {
        return ProcessInfo.processInfo.physicalMemory
    }
    
}
