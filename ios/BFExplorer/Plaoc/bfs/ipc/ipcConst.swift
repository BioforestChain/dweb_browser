//
//  ipcConst.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

enum IPC_MESSAGE_TYPE: Int, Codable {
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
    /** 类型：事件 */
    case event = 6
    /** 应用于序列化反序列化，IpcMessageData默认值 */
    case unknown = 1000
}

struct IPC_DATA_ENCODING: OptionSet, Codable {
    let rawValue: Int
    /** utf8编码的字符串，本质上是binary */
    static let utf8 = IPC_DATA_ENCODING(rawValue: 1 << 1)
    /** base64编码的字符串，本质上是binary */
    static let base64 = IPC_DATA_ENCODING(rawValue: 1 << 2)
    /** 二进制，与utf8/base64 是对等关系 */
    static let binary = IPC_DATA_ENCODING(rawValue: 1 << 3)
}

//struct IPC_RAW_BODY_TYPE: OptionSet, Codable {
//    let rawValue: Int
//    /** 文本 json html 等 */
//    static let text = IPC_RAW_BODY_TYPE(rawValue: 1 << 1)
//    /** 使用文本表示的二进制 */
//    static let base64 = IPC_RAW_BODY_TYPE(rawValue: 1 << 2)
//    /** 二进制 */
//    static let binary = IPC_RAW_BODY_TYPE(rawValue: 1 << 3)
//    /** 流 */
//    static let stream_id = IPC_RAW_BODY_TYPE(rawValue: 1 << 4)
//    /** 文本流 */
//    static let text_stream_id = IPC_RAW_BODY_TYPE(rawValue: IPC_RAW_BODY_TYPE.stream_id.rawValue | IPC_RAW_BODY_TYPE.text.rawValue)
//    /** 文本二进制流 */
//    static let base64_stream_id = IPC_RAW_BODY_TYPE(rawValue: IPC_RAW_BODY_TYPE.stream_id.rawValue | IPC_RAW_BODY_TYPE.base64.rawValue)
//    /** 二进制流 */
//    static let binary_stream_id = IPC_RAW_BODY_TYPE(rawValue: IPC_RAW_BODY_TYPE.stream_id.rawValue | IPC_RAW_BODY_TYPE.binary.rawValue)
//}

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
    var type: IPC_MESSAGE_TYPE { get }
}
struct IpcMessageData: IpcMessage {
    var type: IPC_MESSAGE_TYPE = .unknown
}
struct IpcMessageString: IpcMessage {
    var type: IPC_MESSAGE_TYPE = .unknown
    var data: String
}

typealias Callback<T, R> = (T) -> R?
typealias VoidCallback<R> = () -> R?
typealias AsyncCallback<T, R> = (T) async -> R?
typealias AsyncVoidCallback<R> = () async -> R?

//struct S_MetaBody: Codable {
//    var string: String?
//    var data: Data?
//}
//struct MetaBody: Codable {
//    let type: IPC_RAW_BODY_TYPE
//    let data: S_MetaBody
//}




func dataToBinary(data: IpcEvent.IpcEventData /* String or Data */, encoding: IPC_DATA_ENCODING) -> Data {
    switch encoding {
    case .binary:
        return data.data!
    case .base64:
        return data.string!.fromBase64()!
    case .utf8:
        return data.string!.fromUtf8()!
    default:
        fatalError("unknown encoding")
    }
}

func dataToText(data: IpcEvent.IpcEventData /* String or Data */, encoding: IPC_DATA_ENCODING) -> String {
    switch encoding {
    case .binary:
        return String(data: data.data!, encoding: .utf8)!
    case .base64:
        return String(data: data.string!.fromBase64()!, encoding: .utf8)!
    case .utf8:
        return data.string!
    default:
        fatalError("unknown encoding")
    }
}
