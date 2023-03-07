//
//  ipcConst.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

enum IPC_DATA_TYPE: Int, Codable {
    /** 类型：请求 */
    case request = 0
    /** 类型：响应 */
    case response = 1
    /** 类型：流数据，发送方 */
    case stream_data = 2
    /** 类型：流拉取，请求方 */
    case stream_pull = 3
    /** 类型：流关闭，发送方
     *  可能是发送完成了，也可能是中断了
     */
    case stream_end = 4
    /** 类型：流中断，请求方 */
    case stream_abort = 5
    /** 应用于序列化反序列化，IpcMessageData默认值 */
    case unknown = 1000
}

// 位移枚举
struct IPC_RAW_BODY_TYPE: OptionSet, Codable {
    let rawValue: Int
    /** 文本 json html 等 */
    static let text = IPC_RAW_BODY_TYPE(rawValue: 1 << 1)
    /** 使用文本表示的二进制 */
    static let base64 = IPC_RAW_BODY_TYPE(rawValue: 1 << 2)
    /** 二进制 */
    static let binary = IPC_RAW_BODY_TYPE(rawValue: 1 << 3)
    /** 流 */
    static let stream_id = IPC_RAW_BODY_TYPE(rawValue: 1 << 4)
    /** 文本流 */
    static let text_stream_id = IPC_RAW_BODY_TYPE(rawValue: IPC_RAW_BODY_TYPE.stream_id.rawValue | IPC_RAW_BODY_TYPE.text.rawValue)
    /** 文本二进制流 */
    static let base64_stream_id = IPC_RAW_BODY_TYPE(rawValue: IPC_RAW_BODY_TYPE.stream_id.rawValue | IPC_RAW_BODY_TYPE.base64.rawValue)
    /** 二进制流 */
    static let binary_stream_id = IPC_RAW_BODY_TYPE(rawValue: IPC_RAW_BODY_TYPE.stream_id.rawValue | IPC_RAW_BODY_TYPE.binary.rawValue)
}

struct S_RawData: Codable {
    var string: String?
    var data: Data?
}
struct RawData: Hashable, Codable {

    var type: IPC_RAW_BODY_TYPE
    var data: S_RawData

    static func ==(lhs: RawData, rhs: RawData) -> Bool {
        return lhs.type == rhs.type
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(type.rawValue)
    }
}

enum IPC_ROLE: String, Codable {
    case server = "server"
    case client = "client"
}

/** Ipc消息通用协议 */
protocol IpcMessage: Codable {
    var type: IPC_DATA_TYPE { get }
}
struct IpcMessageData: IpcMessage {
    var type: IPC_DATA_TYPE = .unknown
}
struct IpcMessageString: IpcMessage {
    var type: IPC_DATA_TYPE = .unknown
    var data: String
}

/** message: 只会有两种类型的数据 */
typealias OnIpcMessage = ((IpcMessage, Ipc)) async -> SIGNAL_CTOR?
typealias OnIpcRequestMessage = ((IpcRequest, Ipc)) async -> SIGNAL_CTOR?
typealias Callback<T, R> = (T) -> R?

struct S_MetaBody: Codable {
    var string: String?
    var data: Data?
}
struct MetaBody: Codable {
    let type: IPC_RAW_BODY_TYPE
    let data: S_MetaBody
}
