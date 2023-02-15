//
//  createSignal.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation



class Signal: NSObject {
    
    typealias Callback = (_ message: NSObject? , _ ipc: Ipc?) -> Any
    
    private var cbsDict: [Int: (_ message: NSObject? , _ ipc: Ipc?) -> Any] = [:]
    
    init(callback: @escaping (_ message: NSObject? , _ ipc: Ipc?) -> Any) {
        super.init()
    }
    
    func listen(callback: @escaping (_ message: NSObject? , _ ipc: Ipc?) -> Any) {
        cbsDict[Date().milliStamp] = callback
    }
    
    func emit(message: NSObject? , _ ipc: Ipc?) {
        for (_, value) in cbsDict {
            _ = value(message, ipc)
        }
    }
    
    static func createSignal<T>(callback: T) -> Signal {
        return Signal(callback: callback as! (NSObject?, Ipc?) -> Any)
    }
}
