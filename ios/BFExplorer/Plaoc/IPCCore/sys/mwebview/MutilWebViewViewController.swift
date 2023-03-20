//
//  MutilWebViewViewController.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/8.
//

import UIKit

class PermissionActivity: UIViewController {
    
    let PERMISSION_REQUEST_CODE_PHOTO = 2
    var requestPermissionsResultMap: [Int: RequestPermissionsResult] = [:]
    private var requestPermissionsCodeAcc = 1
    
    override func viewDidLoad() {
        super.viewDidLoad()

    }
    
    func requestPermissions(permissions: [String]) -> RequestPermissionsResult {
        
        let result = RequestPermissionsResult(code: requestPermissionsCodeAcc)
        requestPermissionsCodeAcc += 1
        
        if permissions.count > 0 {
            requestPermissionsResultMap[result.code] = result
            //TODO  请求权限
        } else {
            result.done()
        }
        
        result.waitPromise()
        return result
    }
    
}

class MutilWebViewViewController: PermissionActivity {

    private var remoteMmid: String = ""
    private var controller: MutilWebViewViewController?
    
    override func viewDidLoad() {
        super.viewDidLoad()

    }
    
    func upsetRemoteMmid() {
         
    }
    
}

class RequestPermissionsResult {
    
    var code: Int
    var grants: [String] = []
    var denied: [String] = []
    private let task = PromiseOut<Void>()
    
    var isGranted: Bool {
        return denied.count == 0
    }
    
    init(code: Int) {
        self.code = code
    }
    
    func done() {
        task.resolver(())
    }
    
    func waitPromise() {
        task.waitPromise()
    }
}
