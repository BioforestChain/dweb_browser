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
        print("status.code: \(status.code)")
        if status.code >= 400 {
            fatalError(status.description)
        } else {
            return self
        }
    }
    
    func json<T: Decodable>(_ typeOfT: T.Type) -> T {
//        var data = Data()
//        _ = ok().body.collect(on: HttpServer.app.eventLoopGroup.next()).map { bytebuffer in
//            if var buffer = bytebuffer, buffer.readableBytes > 0 {
//                let _data = buffer.readData(length: buffer.readableBytes)!
//                print(_data)
//                data += _data
//            }
//        }
        
        do {
            var buffer = try ok().body.collect(on: HttpServer.app.eventLoopGroup.next()).wait()
            return try JSONDecoder().decode(typeOfT, from: Data(buffer!.readableBytesView))
        } catch {
            fatalError("JSON decoder error: \(error.localizedDescription)")
        }
    }
    
    func text() -> String? {
        return ok().body.string
    }
    
    func stream() -> InputStream {
//        var data = Data()
//        _ = ok().body.collect(on: HttpServer.app.eventLoopGroup.next()).map { bytebuffer in
//            if var buffer = bytebuffer, buffer.readableBytes > 0 {
//                data.append(buffer.readData(length: buffer.readableBytes)!)
//            }
//        }
        do {
            var buffer = try ok().body.collect(on: HttpServer.app.eventLoopGroup.next()).wait()
            return InputStream(data: Data(buffer!.readableBytesView))
        } catch {
            fatalError("body stream data error: \(error.localizedDescription)")
        }
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

extension BodyStreamWriter {
    func responseStreamWriter(stream: ReadableStream) {
        let bufferSize = 1024
        
//        stream.open()
        defer {
            if stream.streamStatus == .open {
                stream.close()
            }
        }
        
        while stream.available() <= 0 {
            print("responseStreamWriter")
        }
        
        Task {
            while (stream.available()) > 0 {
                if stream.streamStatus == .notOpen {
                    stream.open()
                }
                var data = Data()
                var buffer = [UInt8](repeating: 0, count: bufferSize)
                let bytesRead = stream.read(&buffer, maxLength: bufferSize)
                if bytesRead < 0 {
    //                    _ = writer.write(.error("Error reading from stream" as! Error))
                    _ = self.write(.end)
                } else if bytesRead == 0 {
                    _ = self.write(.end)
                }
                data.append(buffer, count: bytesRead)
                let byteBuffer = ByteBuffer(data: data)
                _ = self.write(.buffer(byteBuffer))
            }
        }
    }
}
