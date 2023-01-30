//
//  OperateMonitor.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/8.
//

import UIKit
import RxSwift
import WebKit


let operateMonitor = OperateMonitor()
class OperateMonitor: NSObject {

    let tabBarMonitor = PublishSubject<Void>()
    let interceptMonitor = PublishSubject<(WKURLSchemeTask,String,String)>()
    let refreshCompleteMonitor = PublishSubject<String>()
    let startAnimationMonitor = PublishSubject<String>()
    let backMonitor = PublishSubject<String>()
    let scanMonitor = PublishSubject<(String,[String:Any])>()
    
}
