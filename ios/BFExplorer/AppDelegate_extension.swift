//
//  AppDelegate_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/8.
//

import Foundation

extension AppDelegate {
    
    func startDwebBrowser() {
        
        let dnsNMM = DnsNMM()
        
        // 安装系统应用
        let jsProcessNMM = JsProcessNMM()
        dnsNMM.install(mm: jsProcessNMM)
        
        let multiWebViewNMM = MultiWebViewNMM()
        dnsNMM.install(mm: multiWebViewNMM)
        
        let httpNMM = HttpNMM()
        dnsNMM.install(mm: httpNMM)
        
        let browserNMM = BrowserNMM()
        dnsNMM.install(mm: browserNMM)
        
        let jmmNMM = JmmNMM()
        dnsNMM.install(mm: jmmNMM)
        
        
        
        let bootNMM = BootNMM(initMmids: [browserNMM.mmid])
        dnsNMM.install(mm: bootNMM)
        
        dnsNMM.bootstrap()
    }
}
