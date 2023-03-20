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
    
    func json<T: Decodable>(_ typeOfT: T.Type) -> T {
        
        do {
            var buffer = try self.body.collect(on: HTTPServer.app.eventLoopGroup.next()).wait()
            return try JSONDecoder().decode(typeOfT, from: Data(buffer!.readableBytesView))
        } catch {
            fatalError("JSON decoder error: \(error.localizedDescription)")
        }
    }
    
    func text() -> String? {
        return self.body.string
    }
    
    func int() -> Int? {
        text() != nil ? Int(text()!) : nil
    }
    
    func int64() -> Int64? {
        text() != nil ? Int64(text()!) : nil
    }
    
    func double() -> Double? {
        text() != nil ? Double(text()!) : nil
    }
    
    func float() -> Float? {
        text() != nil ? Float(text()!) : nil
    }
    
    func boolean() -> Bool {
        return text() == "true"
    }
}
