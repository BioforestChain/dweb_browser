//
//  UsableIpcBodyMapper.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/6.
//

import UIKit

class UsableIpcBodyMapper {

    private var map: [String:IpcBodySender] = [:]
    private let destroySignal = SimpleSignal()
    
    init() {
        
    }
    
    init(map: [String:IpcBodySender]) {
        self.map = map
    }
    
    func add(streamId: String, ipcBody: IpcBodySender) -> Bool {
        
        guard !map.keys.contains(streamId) else { return false }
        map[streamId] = ipcBody
        return true
    }
    
    func get(streamId: String) -> IpcBodySender? {
        return map[streamId]
    }
    
    func remove(streamId: String) {
        let sender = map.removeValue(forKey: streamId)
        guard sender != nil else { return }
        // 如果都删除完了，那么就触发事件解绑
        if map.isEmpty {
            destroySignal.emit(())
            destroySignal.clear()
        }
    }
    
    func onDestroy(cb: @escaping SimpleCallbcak) -> OffListener {
        return destroySignal.listen(cb)
    }
}
