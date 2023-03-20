//
//  cosnt.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation
import HandyJSON
import Vapor



enum IPC_MESSAGE_TYPE: Int {
    /** 类型：请求 */
    case REQUEST            = 0
    /** 类型：相应 */
    case RESPONSE           = 1
    /** 类型：流数据，发送方 */
    case STREAM_DATA     = 2
    /** 类型：流拉取，请求方 */
    case STREAM_PULL        = 3
    /** 类型：流关闭，发送方
         * 可能是发送完成了，也有可能是被中断了
         */
    case STREAM_END         = 4
    /** 类型：流中断，请求方 */
    case STREAM_ABORT       = 5
    /** 类型：事件 */
    case STREAM_EVENT       = 6
    case NONE               = 1000
}

enum IPC_DATA_ENCODING: Int {
    case NONE            = 0
    /** UTF8编码的字符串，本质上是 BINARY */
    case UTF8            = 1
    /** BASE64编码的字符串，本质上是 BINARY */
    case BASE64          = 2
    /** 二进制, 与 UTF8/BASE64 是对等关系*/
    case BINARY          = 3
}

enum IPC_META_BODY_TYPE: Int {
    /** 文本 json html 等 */
    case IPC_META_BODY_TYPE_STREAM_ID     = 0
    /** 内联数据 */
    case IPC_META_BODY_TYPE_INLINE         = 1
    /** 文本 json html 等 */
    case STREAM_WITH_TEXT                  = 2
    /** 使用文本表示的二进制 */
    case STREAM_WITH_BASE64                = 3
    /** 二进制 */
    case STREAM_WITH_BINARY                = 4
    /** 文本 json html 等 */
    case INLINE_TEXT                       = 5
    /** 使用文本表示的二进制 */
    case INLINE_BASE64                     = 6
    /** 二进制 */
    case INLINE_BINARY                     = 7
}

enum IPC_ROLE: String {
    case SERVER = "server"
    case CLIENT = "client"
}


protocol MicroModuleInfo {
    var mmid: String { get }
}

protocol IpcMessage: HandyJSON {
    var type: IPC_MESSAGE_TYPE { get }
}

protocol ValueCallback {
    
    associatedtype T
    func onReceiveValue(value: T)
}

protocol FilePathProtocol {
    
    func onReceiveValue(value: [URI])
}

protocol RequestPermissionCallback {
    
    func onReceiveValue(value: Bool)
}

typealias OnIpcMessage = ((IpcMessage, Ipc)) -> Void
typealias OnIpcrequestMessage = ((IpcRequest,Ipc)) -> Void
typealias OnIpcEventMessage = ((IpcEvent,Ipc)) -> Void
typealias IpcConnect = ((Ipc,Request)) -> Void
typealias closeCallback = (()) -> Void
typealias SimpleCallbcak = (()) -> Any?
typealias OffListener = () -> Bool
typealias AsyncCallback<T, R> = (T) async -> R?
