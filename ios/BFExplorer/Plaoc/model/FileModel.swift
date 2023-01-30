//
//  FileModel.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/20.
//

import UIKit

struct FileModel: Encodable {
    
    var name: String?
    var extname: String?
    var path: String?
    var cwd: String?
    var type: String?
    var isLink: Bool?
    var relativePath: String?
    
}
