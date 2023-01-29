//
//  ReachabilityManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/14.
//

import UIKit
import Network

extension NWInterface.InterfaceType: CaseIterable {
    
    public static var allCases: [NWInterface.InterfaceType] = [
        //未知
        .other,
        .wifi,
        //蜂窝
        .cellular,
        //无网络
        .loopback,
        //有线
        .wiredEthernet
    ]
}

class ReachabilityManager: NSObject {
    
    
    static let shared = ReachabilityManager()
    
    private let queue = DispatchQueue(label: "com.monitor")
    private let monitor: NWPathMonitor
    private(set) var isConnected = false
    private(set) var isExpensive = false
    private(set) var currentType: NWInterface.InterfaceType?
    
    override init() {
        monitor = NWPathMonitor()
    }
    
    func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            guard let strongSelf = self else { return }
            strongSelf.isConnected = path.status != .unsatisfied
            strongSelf.isExpensive = path.isExpensive
            
            strongSelf.currentType = NWInterface.InterfaceType.allCases.filter { path.usesInterfaceType($0) }.first
            
        }
        monitor.start(queue: queue)
    }
    
    func stopMonitoring() {
        monitor.cancel()
    }
    
    func getNetworkStatus() -> String {
        var networkTypeString = ""
        
        switch(self.currentType) {
        case .cellular:
            networkTypeString = "cellular"
        case .wifi:
            networkTypeString = "wifi"
        default:
            networkTypeString = "unknown"
        }
        
        return networkTypeString
    }
}

