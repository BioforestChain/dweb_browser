//
//  MyDnsMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/16.
//

import UIKit
import Vapor

class MyDnsMicroModule: DnsMicroModule {

    var dnsMM: DnsNMM
    var fromMM: MicroModule
    
    init(dnsMM: DnsNMM, fromMM: MicroModule) {
        self.dnsMM = dnsMM
        self.fromMM = fromMM
    }
    
    func install(mm: MicroModule) {
        //TODO作用域保护
        dnsMM.install(mm: mm)
    }
    func uninstall(mm: MicroModule) {
        //TODO作用域保护
        dnsMM.uninstall(mm: mm)
    }
    func connect(mmid: String, reason: Request?) -> ConnectResult? {
        //TODO 权限保护
        return dnsMM.connectTo(fromMM: fromMM, toMmid: mmid, reason: reason ?? Request.new(method: HTTPMethod.GET, url: "file://\(mmid)"))
    }
}


