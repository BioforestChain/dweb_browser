//
//  OrderFunDump.swift
//  DwebBrowserCommon
//
//  Created by instinct on 2024/2/28.
//

import Foundation

public class DwebOrderFunDump {
    
    public static func dumpOrderFileIfNeed() {
        guard DwebConfigInfo.isOrderDump else { return }
        dumpFile()
    }
}


