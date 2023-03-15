//
//  InputStream_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/14.
//

import Foundation

extension InputStream {
    
    func readByteArray() -> [UInt8] {
        
        self.open()
        defer {
            self.close()
        }
        
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        var result: [UInt8] = []
        while self.hasBytesAvailable {
            let length = self.read(buffer, maxLength: bufferSize)
            let data = Data(bytes: buffer, count: length)
            result += [UInt8](data)
        }
        return result
    }
    
    func hasNext() -> Bool {
        self.open()
        defer {
            self.close()
        }
        return self.hasBytesAvailable
    }
    
    func next() -> [UInt8] {
        self.open()
        defer {
            self.close()
        }
        
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        var result: [UInt8] = []
        if hasNext() {
            let length = self.read(buffer, maxLength: bufferSize)
            let data = Data(bytes: buffer, count: length)
            result += [UInt8](data)
        }
        return result
    }
    
    func readByteArray(size: Int) -> [UInt8] {
        
        self.open()
        defer {
            self.close()
        }
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        var result: [UInt8] = []
        var offset: Int = size
        while self.hasBytesAvailable {
            if offset <= 0 {
                break
            }
            let length = self.read(buffer, maxLength: bufferSize)
            offset = size - length
            let data = Data(bytes: buffer, count: length)
            result += [UInt8](data)
        }
        return result
    }
    
    func readInt() -> Int {
        
        let bytes = [UInt8](Array(repeating: 0, count: 4))
        return bytes.toInt()
    }
}

