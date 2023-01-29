//
//  JSInjectManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/13.
//

import JavaScriptCore

class JSInjectManager {
    
    static let shared = JSInjectManager()

    var timers = [String: Timer]()

    func removeTimer(identifier: String) {
        let timer = self.timers.removeValue(forKey: identifier)

        timer?.invalidate()
    }

    func createTimer(callback: JSValue, ms: Double, repeats : Bool) -> String {
        let timeInterval  = ms/1000.0

        let uuid = NSUUID().uuidString

        DispatchQueue.main.async(execute: {
            let timer = Timer.scheduledTimer(timeInterval: timeInterval,
                                             target: self,
                                             selector: #selector(self.callJsCallback),
                                             userInfo: callback,
                                             repeats: repeats)
            self.timers[uuid] = timer
        })

        return uuid
    }

    @objc func callJsCallback(_ timer: Timer) {
        let callback = (timer.userInfo as! JSValue)

        callback.call(withArguments: nil)
    }
    
    func registerInContext(_ context: JSContext) {
        let clearInterval: @convention(block) (String) -> () = { identifier in
            self.removeTimer(identifier: identifier)
        }

        let clearTimeout: @convention(block) (String) -> () = { identifier in
            self.removeTimer(identifier: identifier)
        }

        let setInterval: @convention(block) (JSValue, Double) -> String = { (callback, ms) in
            return self.createTimer(callback: callback, ms: ms, repeats: true)
        }

        let setTimeout: @convention(block) (JSValue, Double) -> String = { (callback, ms) in
            print("haha")
            return self.createTimer(callback: callback, ms: ms, repeats: false)
        }

        context.setObject(clearInterval,
                          forKeyedSubscript: "clearInterval" as NSString)

        context.setObject(clearTimeout,
                          forKeyedSubscript: "clearTimeout" as NSString)

        context.setObject(setInterval,
                          forKeyedSubscript: "setInterval" as NSString)

        context.setObject(setTimeout,
                          forKeyedSubscript: "setTimeout" as NSString)
    }
    
}
