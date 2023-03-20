//
//  MainServer.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/18.
//

import UIKit

struct MainServer {

    //应用文件夹的目录
    var root: String
    //入口文件
    var entry: String
    
    init(root: String, entry: String) {
        self.root = root
        self.entry = entry
    }
}

