//
//  ConfirmConfiguration.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/23.
//

import Foundation
import SwiftyJSON

class ConfirmConfiguration: AlertConfiguration {
    
    var cancelText: String?
    
    override init(dict: JSON) {
        super.init(dict: dict)
        cancelText = dict["cancelText"].stringValue
    }
}
