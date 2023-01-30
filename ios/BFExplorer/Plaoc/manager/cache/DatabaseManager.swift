//
//  DatabaseManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/8/1.
//

import UIKit

class DatabaseManager: NSObject {

    //保存数据
    func preserveData(data:AnyObject, key: AnyObject) {
        
        print("保存到数据库中")
    }
    
    //查询数据
    func enquiriesData(forKey key: AnyObject) -> AnyObject? {
        
        print("从数据库中查询")
        return nil
    }
    //更新数据
    func updataData(data:AnyObject, key: AnyObject) {
        
        print("更新数据库中数据")
    }
    //清除缓存数据
    func cleanCacheData(forKey key: AnyObject) {
        
        print("清除数据库中数据")
    }
}
