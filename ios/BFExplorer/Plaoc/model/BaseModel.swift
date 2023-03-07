//
//  BaseModel.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/4.
//

import UIKit
import HandyJSON

class BaseModel: HandyJSON {

    required init() {
        
    }
}

struct BaseStruct: IpcMessage, HandyJSON {
    
    var type: IPC_DATA_TYPE = .NONE
   
    init() {
        
    }
}


