//
//  ConsoleSwift.swift
//  DwebBrowser
//
//  Created by ui03 on 2023/7/24.
//

import SwiftUI

@objc(ConsoleSwift)
public class ConsoleSwift: NSObject {
    public typealias Inject = (String) -> Void
    public static var inject: Inject?

    override public init() {
        super.init()
    }

    @objc static func injectAction(callback: @escaping Inject) {
        ConsoleSwift.inject = callback
    }

    @objc func testAction() {
        Log("haha")
    }
    
    static func log(_ s: String) {
        guard let injector = inject else {
            return
        }
        
        injector(s)
    }
}
