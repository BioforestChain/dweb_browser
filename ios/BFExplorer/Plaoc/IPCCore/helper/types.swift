//
//  types.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation

protocol MicroModule {
    //TODO 以string + .dweb 的格式
    var mmid: String { get }
    func fetch( input: URLRequest) -> HTTPURLResponse
}

extension MicroModule {
    
    var mmid: String { return "" }
    func fetch( input: URLRequest) -> HTTPURLResponse {
        return HTTPURLResponse()
    }
}

/** TODO
1、types.cts -> RequestInit 类型怎么翻译
2、createSignal.cts -> Signal类的listen方法的return结果是什么
3、const.cts -> RawData是什么类型
4、IpcBody.cts -> stream() -> Blob.stream()
5、urlHelper.cts -> URL_BASE
6、streamAsRawData
**/
