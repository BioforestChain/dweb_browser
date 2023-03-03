//
//  Response.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/2.
//

import Foundation
import Vapor
import Alamofire

extension Response {
    func ok() -> Response {
        if status.code >= 400 {
            fatalError(status.description)
        } else {
            return self
        }
    }
    
    func json<T: Decodable>(_ typeOfT: T.Type) -> T {
        var data = Data()
        _ = ok().body.collect(on: HttpServer.app.eventLoopGroup.next()).map { bytebuffer in
            if var buffer = bytebuffer, buffer.readableBytes > 0 {
                data.append(buffer.readData(length: buffer.readableBytes)!)
            }
        }
        
        do {
            return try JSONDecoder().decode(typeOfT, from: data)
        } catch {
            fatalError("JSON decoder error: \(error.localizedDescription)")
        }
    }
    
    func text() -> String? {
        return ok().body.string
    }
    
    func stream() -> InputStream {
        var data = Data()
        _ = ok().body.collect(on: HttpServer.app.eventLoopGroup.next()).map { bytebuffer in
            if var buffer = bytebuffer, buffer.readableBytes > 0 {
                data.append(buffer.readData(length: buffer.readableBytes)!)
            }
        }
        return InputStream(data: data)
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
