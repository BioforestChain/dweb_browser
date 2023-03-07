//
//  ChangeTools.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/6.
//

import UIKit
import HandyJSON

class ChangeTools: NSObject {

    static func dicValueString(_ dic:[String : Any]) -> String? {
        let data = try? JSONSerialization.data(withJSONObject: dic, options: [])
        let str = String(data: data!, encoding: String.Encoding.utf8)
        return str
    }
    
    static func arrayValueString(_ array:[[String : Any]]) -> String? {
        let data = try? JSONSerialization.data(withJSONObject: array, options: [])
        let str = String(data: data!, encoding: String.Encoding.utf8)
        return str
    }
    
    static func arrayValueString(_ array:[Any]) -> String? {
        let data = try? JSONSerialization.data(withJSONObject: array, options: [])
        let str = String(data: data!, encoding: String.Encoding.utf8)
        return str
    }
    
    static func stringValueDic(_ str: String) -> [String : Any]? {
        let data = str.data(using: String.Encoding.utf8)
        if let dict = try? JSONSerialization.jsonObject(with: data!,
                        options: .mutableContainers) as? [String : Any] {
            return dict
        }

        return nil
    }
    
    static func stringValueArray(_ str: String) -> [[String : Any]]? {
        let data = str.data(using: String.Encoding.utf8)
        if let array = try? JSONSerialization.jsonObject(with: data!,
                        options: .mutableContainers) as? [[String : Any]] {
            return array
        }

        return nil
    }
    
    static func anyValueToString<T>(value: T) -> String where T : Encodable {

        guard let data = try? JSONEncoder().encode(value) else { return "" }
        return String(data: data, encoding: .utf8) ?? ""
    }
    
    static func tempAnyToString(value: Any) -> String? {
        
        if value is Int || value is Float || value is Double || value is Bool {
            return "\(value)"
        }
        
        if value is [Any] {
            return ChangeTools.arrayValueString(value as! [Any]) ?? ""
        }
        
        if value is [String:Any] {
            return ChangeTools.dicValueString(value as! [String : Any]) ?? ""
        }
        return nil
    }
    
    static func jsonToModel(jsonStr: String, _ modelType: HandyJSON.Type) -> BaseModel? {
        
        guard jsonStr.count > 0 else { return nil }
        return modelType.deserialize(from: jsonStr) as? BaseModel
    }
    
    static func jsonArrayToModel(jsonStr: String, _ modelType: HandyJSON.Type) -> [BaseModel]? {
        
        guard jsonStr.count > 0 else { return nil }
        var array: [BaseModel] = []
        guard let data = jsonStr.data(using: .utf8) else { return nil }
        guard let modelList = try? JSONSerialization.jsonObject(with: data) as? [[String:Any]] else { return nil }
        for model in modelList {
            if let obj = ChangeTools.dictionaryToModel(model, modelType) {
                array.append(obj)
            }
        }
        return array
    }
    
    /**
     *  字典转对象
     */
    static func dictionaryToModel(_ dictionStr:[String:Any],_ modelType:HandyJSON.Type) -> BaseModel? {

        guard dictionStr.count > 0 else { return nil }
        return modelType.deserialize(from: dictionStr) as? BaseModel
    }
}
