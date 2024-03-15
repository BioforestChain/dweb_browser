//
//  KotlinComposeMetalRedrawerFix.swift
//  iosApp
//
//  Created by instinct on 2023/12/21.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import QuartzCore

private var associateKey_isNeedFix: Void?

extension CADisplayLink {
        
    var isNeedFix: Bool {
        get {
            return objc_getAssociatedObject(self, &associateKey_isNeedFix) as? Bool ?? false
        }
        set {
            objc_setAssociatedObject(self, &associateKey_isNeedFix, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
    
    @objc
    func myAdd(to runloop: RunLoop, forMode mode: RunLoop.Mode) {
        let toMode: RunLoop.Mode = isNeedFix ? .common : mode
//        Log("CADisplayLink to: \(runloop) mode:\(mode) fix:\(isNeedFix)")
        myAdd(to: runloop, forMode: toMode)
    }
    
    @objc
    class func myInit(target:Any, selector:Selector) -> CADisplayLink {
//        Log("CADisplayLink target: \(target) selector:\(selector)")
        let result = myInit(target: target, selector: selector)
        if selector == NSSelectorFromString("handleDisplayLinkTick") {
            result.isNeedFix = true
        }
        return result
    }
}

class KotlinComposeRedrawerFix {
    
    public class func fix() {
        let originalMethod = class_getInstanceMethod(CADisplayLink.self, #selector(CADisplayLink.add(to:forMode:)))
        let swizzledMethod = class_getInstanceMethod(CADisplayLink.self, #selector(CADisplayLink.myAdd(to:forMode:)))
        method_exchangeImplementations(originalMethod!, swizzledMethod!)
        
        let originalClassMethod = class_getClassMethod(CADisplayLink.self, #selector(CADisplayLink.init(target: selector:)))
        let swizzledClassMethod = class_getClassMethod(CADisplayLink.self, #selector(CADisplayLink.myInit(target: selector:)))
        method_exchangeImplementations(originalClassMethod!, swizzledClassMethod!)
    }

    private func addRunloopObservers() {
        let cfModes = CFRunLoopCopyAllModes(CFRunLoopGetMain())
        let count = CFArrayGetCount(cfModes)
        var index = 0
        while index < count {
            let cfmode = CFArrayGetValueAtIndex(cfModes, index)
            index += 1
            let mode = unsafeBitCast(cfmode, to: CFRunLoopMode.self)
            addObserver(mode: mode)
        }
    }

    private func addObserver(mode: CFRunLoopMode) {
        let observerRef = CFRunLoopObserverCreateWithHandler(kCFAllocatorDefault, CFRunLoopActivity.allActivities.rawValue, true, 0) { [unowned self] bserver, activity in
            self.logRunloop(tag: "\(mode)", activity: CFRunLoopActivity(rawValue: activity))
        }
        CFRunLoopAddObserver(CFRunLoopGetMain(), observerRef, mode)
        
    }
        
    private func logRunloop(tag:String, activity: CFRunLoopActivity) {
        switch activity {
        case .entry:
            Log("Runloop [\(tag)] enter")
        case .beforeTimers:
            Log("Runloop [\(tag)] beforeTimers")
        case .beforeSources:
            Log("Runloop [\(tag)] beforeSources")
        case .beforeWaiting:
            Log("Runloop [\(tag)] beforeWaiting")
        case .afterWaiting:
            Log("Runloop [\(tag)] afterWaiting")
        case .exit:
            Log("Runloop [\(tag)] exit")
        case .allActivities:
            Log("Runloop [\(tag)] allActivities")
        default:
            Log("Runloop [\(tag)] unknow:\(activity)")
        }
    }
}


