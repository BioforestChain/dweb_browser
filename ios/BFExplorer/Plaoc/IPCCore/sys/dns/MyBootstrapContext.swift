//
//  MyBootstrapContext.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/16.
//

import UIKit

class MyBootstrapContext: BootstrapContext {
    
    var dns: DnsMicroModule

    init(dns: MyDnsMicroModule) {
        self.dns = dns
    }
}
