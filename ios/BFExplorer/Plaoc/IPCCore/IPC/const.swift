//
//  cosnt.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation
import HandyJSON

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

typealias OnIpcMessage = ((IpcMessage, Ipc)) -> Void
typealias OnIpcrequestMessage = ((IpcRequest,Ipc)) -> Void
typealias OnIpcEventMessage = ((IpcEvent,Ipc)) -> Void
typealias closeCallback = (()) -> Any
typealias SimpleCallbcak = (()) -> Any
typealias OffListener = () -> Bool
