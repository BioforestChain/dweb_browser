//
//  PromptConfiguration.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/23.
//

import Foundation
import SwiftyJSON

class PromptConfiguration: AlertConfiguration {
    
    var label: String?
    var defaultValue: String?
    var cancelText: String?
    
    override init(dict: JSON) {
        super.init(dict: dict)
        label = dict["label"].stringValue
        defaultValue = dict["defaultValue"].stringValue
        cancelText = dict["cancelText"].stringValue
    }
}
