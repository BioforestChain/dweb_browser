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
        
        enum Status {
            case none
            case launched (Bool)
            case actived (Bool)
            case unactived (Bool)
            case terminate
        }
        
        enum StatusEvent {
            case didLaunched
            case didActived
            case didUnactived
            case didRender
            case willTerminated
        }
                
        static func updateStatus(by event: StatusEvent, status: Status) -> Status {
             switch (status, event) {
                case (_, .didLaunched): return .launched(false)
                case (_, .willTerminated): return .terminate
                case let (.launched(rended), .didActived): return .actived(rended)
                case (.launched(_), .didRender): return .launched(true)
                case let (.launched(rended), .didUnactived): return .unactived(rended)
                case (.actived(_), .didRender): return .actived(true)
                case let (.actived(rended), .didUnactived): return .unactived(rended)
                case (.unactived(_), .didRender): return .unactived(true)
                case let (.unactived(rended), .didActived): return .actived(rended)
                default:
                 return status
            }
        }
        
        static func match(for event: StatusEvent, status: Status) -> Bool {
            switch status {
                case .launched(false): event == .didLaunched
                case .actived(false): event == .didActived
                case .unactived(false): event == .didUnactived
                case .terminate: event == .willTerminated
                case .launched(true), .actived(true), .unactived(true): event == .didRender
                default: false
            }
        }
    }
    
    
    actor TaskStore {
        typealias Action = ()->Void
        private typealias Task = (Record.StatusEvent, Action)
        
        private lazy var tasks: [Task] = {
            [Task]()
        }()
        
        func register(_ for: Record.StatusEvent, action: @escaping Action) {
            tasks.append((`for`, action))
        }
        
        func doRegistedAction(_ status : Record.Status) {
            Log("action for: \(status)")
            tasks.lazy.filter { Record.match(for: $0.0, status: status) }.forEach { $0.1() }
            tasks.removeAll { Record.match(for: $0.0, status: status) }
        }
    }
    
    static let shared = DwebLifeStatusCenter()
    
    
    private(set) var status: Record.Status = .none {
        didSet {
            doStatusActions()
        }
    }
    
    private lazy var taskStore: TaskStore = {
        TaskStore()
    }()
    
    init() {
        self.addObservers()
    }
    
    func register(_ for: Record.StatusEvent, action: @escaping TaskStore.Action) {
        Task {
            await taskStore.register(`for`, action: action)
            if Record.match(for: `for`, status: status) {
                await taskStore.doRegistedAction(status)
            }
        }
    }
    
    private func doStatusActions() {
        Task {
            await taskStore.doRegistedAction(status)
        }
    }
    
    private func addObservers() {
        NotificationCenter.default.addObserver(self, selector: #selector(appDidLaunched), name: UIApplication.didFinishLaunchingNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appDidActived), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appDidUnActived), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appWillTerminated), name: UIApplication.willTerminateNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appDidRended), name: NSNotification.Name.DwebAppDidRendedNotificationName, object: nil)
    }
    
    @objc private func appDidLaunched() {
        Log()
        status = Record.updateStatus(by: .didLaunched, status: status)
    }
    
    @objc private func appDidActived() {
        Log()
        status = Record.updateStatus(by: .didActived, status: status)
    }
    
    @objc private func appDidUnActived() {
        Log()
        status = Record.updateStatus(by: .didUnactived, status: status)
    }
    
    @objc private func appWillTerminated() {
        Log()
        status = Record.updateStatus(by: .willTerminated, status: status)
    }
    
    @objc private func appDidRended() {
        Log()
        status = Record.updateStatus(by: .didRender, status: status)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
