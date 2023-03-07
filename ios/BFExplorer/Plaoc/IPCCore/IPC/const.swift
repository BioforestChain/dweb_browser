//
//  cosnt.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation
import HandyJSON

enum IPC_DATA_TYPE {
    
    // /** 特殊位：结束符 */
      // END = 1,
      /** 类型：请求 */
    case REQUEST
      /** 类型：相应 */
    case RESPONSE
    /** 类型：流数据，发送方 */
    case STREAM_MESSAGE
      /** 类型：流拉取，请求方 */
    case STREAM_PULL
      /** 类型：流关闭，发送方
       * 可能是发送完成了，也有可能是被中断了
       */
    case STREAM_END
      /** 类型：流中断，请求方 */
    case STREAM_ABORT
    /** 类型：事件 */
    case STREAM_EVENT
    
    case NONE
}

enum IPC_RAW_BODY_TYPE: Int {
    /** 文本 json html 等 */
    case TEXT = 2
      /** 使用文本表示的二进制 */
    case BASE64 = 4
      /** 二进制 */
    case BINARY = 8
      /** 流 */
    case STREAM_ID = 16
      /** 文本流 */
    case TEXT_STREAM_ID = 18
      /** 文本二进制流 */
    case BASE64_STREAM_ID = 20
      /** 二进制流 */
    case BINARY_STREAM_ID = 24
}

enum IPC_DATA_ENCODING {
    
    /** 文本 json html 等 */
    case UTF8

    /** 使用文本表示的二进制 */
    case BASE64

    /** 二进制 */
    case BINARY
}

enum IPC_ROLE: String {
    case SERVER = "server"
    case CLIENT = "client"
}

class MetaBody: NSObject, HandyJSON {
    
    var type: IPC_RAW_BODY_TYPE?
    var data: Any?
    var ipcUid: Int = 0
    
    override required init() {
        
    }
    
    init(type: IPC_RAW_BODY_TYPE?, data: Any?, ipcUid: Int) {
        self.type = type
        self.data = data
        self.ipcUid = ipcUid
    }

}



protocol IpcMessage: HandyJSON {
    var type: IPC_DATA_TYPE { get }
}

typealias OnIpcMessage = ((IpcMessage, Ipc)) -> Any?
typealias OnIpcrequestMessage = ((IpcRequest,Ipc)) -> Any?
typealias closeCallback = (()) -> Any
typealias SimpleCallbcak = (()) -> Any
typealias OffListener = () -> Bool
