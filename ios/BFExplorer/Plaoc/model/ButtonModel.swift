//
//  ButtonModel.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/22.
//

import Foundation
import SwiftyJSON

struct ButtonModel {
    
    var iconModel: IconModel?
    var onClickCode: String?
    var disabled: Bool?
    
    init(dict: JSON) {
        iconModel = IconModel(dict: dict["icon"])
        onClickCode = dict["onClickCode"].stringValue
        disabled = dict["disabled"].boolValue
    }
    
    var buttonDict: [String:Any] {
        return ["icon":iconModel?.iconDict,
                "onClickCode":onClickCode,
                "disabled":disabled]
    }
}

struct IconModel {
    
    var source: String?
    var type: String?
    var description: String?
    var size: String?
    
    init(dict: JSON) {
        source = dict["source"].stringValue
        type = dict["type"].stringValue
        description = dict["description"].stringValue
        size = dict["size"].stringValue
    }
    
    var iconDict: [String:String?] {
        return ["source":source,
                "type":type,
                "description":description,
                "size":size]
    }
}
