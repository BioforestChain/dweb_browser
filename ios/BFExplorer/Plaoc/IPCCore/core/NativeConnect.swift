//
//  ConnectResult.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import UIKit
import Vapor

/**
 * 两个模块的连接结果：
 *
 * 1. fromIpc 是肯定有的，这个对象是我们当前的上下文发起连接得来的通道，要与 toMM 通讯都需要通过它
 * 1. toIpc 则不一定，远程模块可能是自己创建了 Ipc，我们的上下文拿不到这个内存对象
 */

struct ConnectResult {

    var ipcForFromMM: Ipc
    var ipcForToMM: Ipc?
    
    init(ipcForFromMM: Ipc, ipcForToMM: Ipc? = nil) {
        self.ipcForFromMM = ipcForFromMM
        self.ipcForToMM = ipcForToMM
    }
}


typealias ConnectAdapter = (_ fromMM: MicroModule, _ toMM: MicroModule, _ reason: Request) -> ConnectResult?

var connectAdapterManager = AdapterManager<ConnectAdapter>()

/** 外部程序与内部程序建立链接的方法 */
func connectMicroModules(fromMM: MicroModule, toMM: MicroModule, reason: Request) -> ConnectResult {
    
    for connectAdapter in connectAdapterManager.adapters {
        let ipc = connectAdapter(fromMM, toMM, reason)
        if ipc != nil {
            return ipc!
        }
    }
    fatalError("no support connect MicroModules, from:\(fromMM.mmid) to:\(toMM.mmid)")
}
