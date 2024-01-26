//
//  NetworkPermission.swift
//  DwebBrowser
//
//  Created by ui06 on 7/24/23.
//

import Network
import Observation

@Observable public class NetworkManager {
    public var isNetworkAvailable = true
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")

    public init() {
        checkNetworkPermission()
    }
    
    public func checkNetworkPermission() {
        monitor.start(queue: queue)
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
//                 Log( "network status is \(path.status)")
                self?.isNetworkAvailable = path.status == .satisfied
            }
        }
    }
    deinit {
        monitor.cancel()
    }
}
