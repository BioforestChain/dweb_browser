//
//  Snowflake.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/28.
//

import Foundation
import CoreVideo


public typealias SnowflakeID = UInt64

private struct SnowflakeConfig {
    //占位
    static let symbolBits: UInt32 = 1
    //时间长度
    static let timeBits: UInt32 = 41
    //机房ID
    static let IDCBits: UInt32 = 5
    //机器ID
    static let machineBits: UInt32 = 5
    //同一毫秒内序列所占长度
    static let sequenceBits: UInt32 = 12
}

public final class SnowflakeSwift {
    
    private var machine: UInt32
    private var IDC: UInt32
    private var sequence: UInt32
    private var publishMillisecond: UInt64  //发布的时间
    private var lastGeneralMillisecond: UInt64   //单位毫秒
    
    //WARN: publishMillisecond推荐使用固定值，如果使用Date().timeIntervalSince1970 * 1000 自动获取，会导致时间差重复
    init(publishMillisecond: UInt64 = 1662278876498, IDCID: UInt32, machineID: UInt32) {
        
        self.publishMillisecond = publishMillisecond
        self.lastGeneralMillisecond = publishMillisecond
        self.IDC = IDCID & UInt32(1 << SnowflakeConfig.IDCBits - 1)
        self.machine = machineID & UInt32(1 << SnowflakeConfig.machineBits - 1)
        self.sequence = 0
    }
}

extension SnowflakeSwift {
    
    func nextID() -> UInt64? {
        var currentTime = UInt64(Date().timeIntervalSince1970 * 1000)
        if lastGeneralMillisecond < currentTime {
            lastGeneralMillisecond = currentTime
            sequence = 0
        } else if lastGeneralMillisecond == currentTime {
            sequence = (sequence + 1) & UInt32(1 << SnowflakeConfig.sequenceBits - 1)
            if sequence == 0 {
                usleep(1000)
                currentTime = UInt64(Date().timeIntervalSince1970 * 1000)
                lastGeneralMillisecond = currentTime
            }
        } else {
            //时钟回拨，交给业务处理
            return nil
        }
        
        //组装ID
        let timeParameter = UInt64(lastGeneralMillisecond - publishMillisecond)
        let timeOffset = UInt64(SnowflakeConfig.IDCBits + SnowflakeConfig.machineBits + SnowflakeConfig.sequenceBits)
        
        let idcParameter = UInt64(self.IDC)
        let idcOffset = UInt64(SnowflakeConfig.machineBits + SnowflakeConfig.sequenceBits)
        
        let machineParameter = UInt64(self.machine)
        let machineOffset = UInt64(SnowflakeConfig.sequenceBits)
        
        let result = UInt64(timeParameter << timeOffset) | UInt64(idcParameter << idcOffset) | UInt64(machineParameter << machineOffset) | UInt64(self.sequence)
        return result
    }
    
    func time(id: SnowflakeID) -> UInt64 {
        let timeOffset = UInt64(SnowflakeConfig.IDCBits + SnowflakeConfig.machineBits + SnowflakeConfig.sequenceBits)
        return UInt64(id >> timeOffset) + publishMillisecond
    }
    
    func IDC(id: SnowflakeID) -> UInt32 {
        let step = UInt64(id << UInt64(SnowflakeConfig.timeBits + SnowflakeConfig.symbolBits))
        return UInt32(step >> UInt64(SnowflakeConfig.timeBits + SnowflakeConfig.machineBits + SnowflakeConfig.sequenceBits + SnowflakeConfig.symbolBits))
    }
    
    func machine(id: SnowflakeID) -> UInt32 {
        let step = UInt64(id << UInt64(SnowflakeConfig.timeBits + SnowflakeConfig.IDCBits + SnowflakeConfig.sequenceBits))
        return UInt32(step >> UInt64(SnowflakeConfig.IDCBits + SnowflakeConfig.timeBits + SnowflakeConfig.sequenceBits + SnowflakeConfig.symbolBits))
    }
}
