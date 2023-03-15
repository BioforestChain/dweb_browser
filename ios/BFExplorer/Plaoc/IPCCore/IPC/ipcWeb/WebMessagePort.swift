//
//  WebMessagePort.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import UIKit
import Combine

class WebMessagePort: Hashable {
    let name: String
    let role: PortRole
    
    init(name: String, role: PortRole) {
        self.name = name
        self.role = role
    }
    
    func postMessage(_ message: String?) {
        if role == .port1 {
            NotificationCenter.default.post(name: Notification.Name(name + "_port2"), object: message)
        } else {
            NotificationCenter.default.post(name: Notification.Name(name + "_port1"), object: message)
        }
    }
    
    private var cancellable: AnyCancellable?
    
    func onMessage(_ callback: @escaping AsyncCallback<String, Any>) {
        var notiName = name
        
        if role == .port1 {
            notiName += "_port1"
        } else {
            notiName += "_port2"
        }
        
        let publisher = NotificationCenter.default.publisher(for: Notification.Name(notiName), object: nil)
        
        cancellable = publisher.sink { noti in
            if let message = noti.object as? String {
                Task {
                    _ = await callback(message)
                }
            }
        }
    }
    
    func close() {
        cancellable?.cancel()
    }
    
    deinit {
        cancellable?.cancel()
    }
    
    enum PortRole: String {
        case port1 = "port1"
        case port2 = "port2"
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(name)
        hasher.combine(role.rawValue)
    }
    
    static func ==(lhs: WebMessagePort, rhs: WebMessagePort) -> Bool {
        lhs.name == rhs.name && lhs.role.rawValue == rhs.role.rawValue
    }
}
