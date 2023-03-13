//
//  InputStream_extension.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/13.
//

import Foundation

extension InputStream {
    func readInt() -> Int? {
        let maxReadLength = 4
        var buffer = [UInt8](repeating: 0, count: maxReadLength)
        if self.hasBytesAvailable {
            let numberOfByteRead: Int = read(&buffer, maxLength: maxReadLength)
            
            if numberOfByteRead != maxReadLength {
                fatalError("fail to read int(4 byte)")
            }
        }
        
        return Data(bytes: buffer, count: maxReadLength).toInt()
    }
    
    func readData(size: Int) -> Data {
        var buffer = [UInt8](repeating: 0, count: size)
        
        if self.hasBytesAvailable {
            let numberOfByteRead = read(&buffer, maxLength: size)
            
            if numberOfByteRead != size {
                fatalError("fail to read bytes(\(numberOfByteRead)/\(size) byte) in stream")
            }
        }
        
        return Data(bytes: buffer, count: size)
    }
    
    func readData() -> Data {
        var data = Data()
        while self.hasBytesAvailable {
            var buffer = [UInt8](repeating: 0, count: 1024)
            let numberOfByteRead = read(&buffer, maxLength: 1024)
            
            if numberOfByteRead <= 0 {
                break
            }
            
            data += Data(bytes: buffer, count: numberOfByteRead)
        }
        
        return data
    }
}
