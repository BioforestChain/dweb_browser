//
//  main.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/6.
//

import Foundation

func startDwebBrowser() async {
    
    
    let dnsNMM = DnsNMM()
    
    /// 安装系统后应用
    let jsProcessNMM = JsProcessNMM()
    let multiWebViewNMM = MultiWebViewNMM()
    let httpNMM = HttpNMM()
    dnsNMM.install(jsProcessNMM)
    dnsNMM.install(multiWebViewNMM)
    dnsNMM.install(httpNMM)
    
    /// 安装系统桌面
    let browserNMM = BrowserNMM()
    dnsNMM.install(browserNMM)
    
    /// 安装Jmm
    let jmmNMM = JmmNMM()
    dnsNMM.install(jmmNMM)
    
    /// 安装用户应用
    let desktopJMM = DesktopJMM()
    dnsNMM.install(desktopJMM)
    
    /**
     *
     * browserNMM.mmid,
     * desktopJMM.mmid,
     * cotDemoJMM.mmid,
     * cotJMM.mmid,
     * toyJMM.mmid,
     */
    let bootMmidList: Set<Mmid> = [
        browserNMM.mmid,
//        desktopJMM.mmid
    ]
    
    /// 启动程序
    let bootNMM = BootNMM(initMmids: bootMmidList)
    dnsNMM.install(bootNMM)
    
    /// 启动
    await dnsNMM.bootstrap()
}
