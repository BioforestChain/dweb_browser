//
//  Response_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/9.
//

import Vapor

extension Response {
    
    func stream() -> InputStream? {
        
        guard let data = self.body.data else { return nil }
        return InputStream(data: data)
    }
}
