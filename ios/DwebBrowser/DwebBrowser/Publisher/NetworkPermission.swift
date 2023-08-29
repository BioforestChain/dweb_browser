//
//  NetworkPermission.swift
//  DwebBrowser
//
//  Created by ui06 on 7/24/23.
//

import Network
import SwiftUI

class NetworkManager: ObservableObject {
    @Published var isNetworkAvailable = true
    private var monitor: NWPathMonitor? // 声明为实例变量

    init() {
        checkNetworkPermission()
    }

    func checkNetworkPermission() {
        monitor = NWPathMonitor()
        let queue = DispatchQueue(label: "NetworkMonitor")
        monitor?.start(queue: queue)
        monitor?.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                printWithDate( "network permission \(path.status)")
                self?.isNetworkAvailable = path.status == .satisfied
            }
        }
    }
    deinit {
        monitor?.cancel()
    }
}

