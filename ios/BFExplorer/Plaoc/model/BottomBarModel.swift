//
//  BottomBarModel.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/6/23.
//

import Foundation
import SwiftyJSON

struct BottomBarModel {
    
    var iconModel: IconModel?
    var onClickCode: String?
    var titleString: String?
    var disabled: Bool?
    var selected: Bool?
    var selectable: Bool?   //0 不可选中,不修改其他按钮状态。1 可选中，修改其他按钮状态
    var colors: ColorModel?
    
    init(dict: JSON) {
        iconModel = IconModel(dict: dict["icon"])
        onClickCode = dict["onClickCode"].stringValue
        titleString = dict["label"].stringValue
        disabled = dict["disabled"].boolValue
        selected = dict["selected"].boolValue
        selectable = dict["selectable"].boolValue
        colors = ColorModel(dict: dict["colors"])
    }
    
    var buttonDict: [String:Any] {
        return ["icon":iconModel?.iconDict,
                "colors":colors?.colorDict,
                "onClickCode":onClickCode,
                "titleString":titleString,
                "disabled":disabled,
                "selected":selected,
                "selectable":selectable]
    }
}

struct ColorModel {
    
    var indicatorColor: String?
    var iconColor: String?
    var iconColorSelected: String?
    var textColor: String?
    var textColorSelected: String?
    
    init(dict: JSON) {
        
        indicatorColor = dict["indicatorColor"].stringValue
        iconColor = dict["iconColor"].stringValue
        iconColorSelected = dict["iconColorSelected"].stringValue
        textColor = dict["textColor"].stringValue
        textColorSelected = dict["textColorSelected"].stringValue
    }
    
    var colorDict: [String:String?] {
        return ["indicatorColor":indicatorColor,
                "iconColor":iconColor,
                "iconColorSelected":iconColorSelected,
                "textColor":textColor,
                "textColorSelected":textColorSelected]
    }
}
