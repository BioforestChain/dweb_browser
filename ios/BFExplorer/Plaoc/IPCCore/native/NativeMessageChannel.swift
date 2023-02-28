//
//  NativeMessageChannel.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import Combine

struct NativeMessageChannel<T1,T2> {

    private let closePo = PromiseOut<Void>()
    private let channel1 = PassthroughSubject<T1, Never>()
    private let channel2 = PassthroughSubject<T2, Never>()
    
    let port1: NativePort<T1,T2>!
    let port2: NativePort<T2,T1>!
    
    init() {
        port1 = NativePort(channel_in: channel1, channel_out: channel2, closePo: closePo)
        port2 = NativePort(channel_in: channel2, channel_out: channel1, closePo: closePo)
    }
}
