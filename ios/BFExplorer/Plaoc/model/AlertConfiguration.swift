//
//  AlertModel.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/23.
//

import Foundation
import SwiftyJSON

class AlertConfiguration: NSObject {
    var title: String?
    var content: String?
    var confirmText: String?
    var cancelFunc: String?
    var confirmFunc: String?
    var dismissOnBackPress: Bool?
    var dismissOnClickOutside: Bool?
    
    init(dict: JSON) {
        super.init()
        title = dict["title"].stringValue
        content = dict["message"].stringValue
        confirmText = dict["confirmText"].stringValue
        cancelFunc = dict["cancelFunc"].stringValue
        confirmFunc = dict["confirmFunc"].stringValue
        dismissOnBackPress = dict["dismissOnBackPress"].boolValue
        dismissOnClickOutside = dict["dismissOnClickOutside"].boolValue
    }
}
