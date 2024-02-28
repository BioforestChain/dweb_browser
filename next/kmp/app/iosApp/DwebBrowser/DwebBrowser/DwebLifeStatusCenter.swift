//
//  DwebLifeStatusCenter.swift
//  DwebBrowser
//
//  Created by instinct on 2024/1/31.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import UIKit

extension NSNotification.Name {
    static let DwebAppDidRendedNotificationName = NSNotification.Name("DwebAppDidRendedNotificationName")
}

extension DwebLifeStatusCenter {
    func postDidRendedNotification() {
        NotificationCenter.default.post(name: NSNotification.Name.DwebAppDidRendedNotificationName, object: nil)
    }
}

class DwebLifeStatusCenter {
        
    struct Record {
        
        enum Status: Equatable {
            case none
            case launched (Bool)
            case actived (Bool)
            case unactived (Bool)
            case terminate
            
            static func += (left: inout Status, right: Event) {
                left = Record.updateStatus(by: right, status: left)
            }
            
            static func == (left: Status, right: Event) -> Bool {
                return Record.match(for: right, status: left)
            }
        }
        
        enum Event: CaseIterable, Equatable {
            case didLaunched
            case didActived
            case didUnactived
            case didRended
            case willTerminated
        }
        
        private static func updateStatus(by event: Event, status: Status) -> Status {
             switch (status, event) {
                case (.terminate, _): return .terminate
                case (_, .didLaunched): return .launched(false)
                case (_, .willTerminated): return .terminate
                case let (.launched(rended), .didActived): return .actived(rended)
                case (.launched(_), .didRended): return .launched(true)
                case let (.launched(rended), .didUnactived): return .unactived(rended)
                case (.actived(_), .didRended): return .actived(true)
                case let (.actived(rended), .didUnactived): return .unactived(rended)
                case (.unactived(_), .didRended): return .unactived(true)
                case let (.unactived(rended), .didActived): return .actived(rended)
                default:
                 Log("Invaild: \(status) + \(event)")
                 return status
            }
        }
        
        private static func match(for event: Event, status: Status) -> Bool {
            switch status {
                case .launched(false): event == .didLaunched
                case .actived(false): event == .didActived
                case .unactived(false): event == .didUnactived
                case .terminate: event == .willTerminated
                case .launched(true), .actived(true), .unactived(true): event == .didRended
                default: false
            }
        }
    }
    
    
    actor TaskStore {
        typealias Action = ()->Void
        private typealias Task = (Record.Event, Action)
        
        private(set) var status: Record.Status = .none

        private lazy var tasks: [Task] = {
            [Task]()
        }()
        
        func register(_ for: Record.Event, action: @escaping Action) {
            if status == `for` {
                action()
                return
            }
            tasks.append((`for`, action))
        }
        
        func reduce(_ event: Record.Event) {
            Log("\(status) + \(event) ==> ", terminator: "")
            status += event
            LogRaw("\(status)")
            doRegistedAction(status)
        }
        
        private func doRegistedAction(_ status : Record.Status) {
            Log("action for: \(status)")
            tasks.lazy.filter { status == $0.0 }.forEach { $0.1() }
            tasks.removeAll { status == $0.0 }
        }
    }
    
    static let shared = DwebLifeStatusCenter()

    private lazy var taskStore: TaskStore = {
        TaskStore()
    }()
    
    init() {
        self.addObservers()
    }
    
    func register(_ for: Record.Event, action: @escaping TaskStore.Action) {
        Task {
            await taskStore.register(`for`, action: action)
        }
    }
    
    private func addObservers() {
        NotificationCenter.default.addObserver(self, selector: #selector(appDidLaunched), name: UIApplication.didFinishLaunchingNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appDidActived), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appDidUnActived), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appWillTerminated), name: UIApplication.willTerminateNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appDidRended), name: NSNotification.Name.DwebAppDidRendedNotificationName, object: nil)
    }
    
    private func doNotificationHandle(_ event: Record.Event) {
        Task {
            await taskStore.reduce(event)
        }
    }
    
    @objc private func appDidLaunched() {
        Log()
        doNotificationHandle(.didLaunched)
    }
    
    @objc private func appDidActived() {
        Log()
        doNotificationHandle(.didActived)
    }
    
    @objc private func appDidUnActived() {
        Log()
        doNotificationHandle(.didUnactived)
    }
    
    @objc private func appWillTerminated() {
        Log()
        doNotificationHandle(.willTerminated)
    }
    
    @objc private func appDidRended() {
        Log()
        doNotificationHandle(.didRended)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
