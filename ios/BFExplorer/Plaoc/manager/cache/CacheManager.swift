//
//  CacheManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/8/1.
//

import UIKit

class CacheManager: NSObject {

    static let shared = CacheManager()
    enum cacheType {
        case memory  //内存
        case database  //数据库
        case disk  //硬盘
    }
    
    private let memoryManager = MemoryManager()
    private let databaseManager = DatabaseManager()
    private let diskManager = DiskManager()
    
    //保存数据
    func preserveData(data:AnyObject, key: AnyObject, type: cacheType = .memory) {
        
        if type == .memory {
            memoryManager.preserveData(data: data, key: key)
        } else if type == .disk {
            diskManager.preserveData(data: data, key: key)
        } else {
            databaseManager.preserveData(data: data, key: key)
        }
    }
    //查询数据
    func enquiriesData(forKey key: AnyObject, type: cacheType = .memory) -> AnyObject? {
        
        if type == .memory {
            return memoryManager.enquiriesData(forKey: key)
        } else if type == .disk {
            return diskManager.enquiriesData(forKey: key)
        } else {
            return databaseManager.enquiriesData(forKey: key)
        }
    }
    //更新数据
    func updataData(data:AnyObject, key: AnyObject, type: cacheType = .memory) {
        
        if type == .memory {
            memoryManager.updataData(data: data, key: key)
        } else if type == .disk {
            diskManager.updataData(data: data, key: key)
        } else {
            databaseManager.updataData(data: data, key: key)
        }
    }
    //清除缓存数据
    func cleanCacheData(forKey key: AnyObject, type: cacheType = .memory) {
        
        if type == .memory {
            memoryManager.cleanCacheData(forKey: key)
        } else if type == .disk {
            diskManager.cleanCacheData(forKey: key)
        } else {
            databaseManager.cleanCacheData(forKey: key)
        }
    }
}
