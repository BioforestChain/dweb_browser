//
//  BootstrapContext.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/10.
//

import Foundation
import Vapor

protocol DnsMicroModule {
    /**
     * 动态安装应用
     */
    func install(mm: MicroModule)
    /**
     * 动态卸载应用
     */
    func uninstall(mm: MicroModule)
    /**
     * 与其它应用建立连接
     */
    func connect(mmid: Mmid, reason: Request?) async -> ConnectResult
}

protocol BootStrapContext {
    var dns: DnsMicroModule { get }
}
