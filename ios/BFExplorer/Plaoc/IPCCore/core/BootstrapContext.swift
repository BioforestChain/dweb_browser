//
//  BootstrapContext.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import Foundation
import Vapor


protocol BootstrapContext {
    var dns: DnsMicroModule { get }
}


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
    func connect(mmid: String, reason: Request?) -> ConnectResult?
}


extension DnsMicroModule {
    
    func install(mm: MicroModule) { }
    func uninstall(mm: MicroModule) { }
    func connect(mmid: String, reason: Request?) -> ConnectResult? {
        return nil
    }
}
