//
//  cosnt.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation

enum IPC_DATA_TYPE {
    
    // /** 特殊位：结束符 */
      // END = 1,
      /** 类型：请求 */
    case REQUEST
      /** 类型：相应 */
    case RESPONSE
      /** 类型：流数据，发送方 */
    case STREAM_DATA
      /** 类型：流拉取，请求方 */
    case STREAM_PULL
      /** 类型：流关闭，发送方
       * 可能是发送完成了，也有可能是被中断了
       */
    case STREAM_END
      /** 类型：流中断，请求方 */
    case STREAM_ABORT
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

enum IPC_ROLE: String {
    case SERVER = "server"
    case CLIENT = "client"
}

protocol IpcMessage {}
extension IpcRequest: IpcMessage {}
extension IpcResponse: IpcMessage {}
extension IpcStreamData: IpcMessage {}
extension IpcStreamPull: IpcMessage {}
extension IpcStreamEnd: IpcMessage {}
extension IpcStreamAbort: IpcMessage {}


//typealias IpcMessage = Any

typealias OnIpcMessage = ((IpcMessage, Ipc)) -> Any
typealias OnIpcrequestMessage = ((IpcRequest,Ipc)) -> Any
typealias closeCallback = (()) -> Any
