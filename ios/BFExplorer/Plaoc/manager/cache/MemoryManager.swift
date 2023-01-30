//
//  MemoryManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/8/1.
//

import UIKit

class MemoryManager: NSObject {

    private var cache = NSCache<AnyObject, AnyObject>()
    
    override init() {
        super.init()
        cache.totalCostLimit = 5 * 1024 * 1024
    }
    //保存数据
    func preserveData(data:AnyObject, key: AnyObject) {
        
        cache.setObject(data, forKey: key as AnyObject)
        print("保存到内存中")
    }
    
    //查询数据
    func enquiriesData(forKey key: AnyObject) -> AnyObject? {
        print("从内存中查询")
        return cache.object(forKey: key)
    }
    //更新数据
    func updataData(data:AnyObject, key: AnyObject) {
        cache.setObject(data, forKey: key)
        print("更新内存中数据")
    }
    //清除缓存数据
    func cleanCacheData(forKey key: AnyObject) {
        cache.removeObject(forKey: key)
        print("清除内存中数据")
    }
}
