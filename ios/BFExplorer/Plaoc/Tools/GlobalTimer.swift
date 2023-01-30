//
//  GlobalTimer.swift
//  BFS
//
//  Created by ui03 on 2022/8/31.
//

import UIKit

class GlobalTimer: NSObject {

    static let shared = GlobalTimer()
    private var timer: DispatchSourceTimer?
    private let interval: Int = 60
    
    func StartTimer() {
        
        timer = DispatchSource.makeTimerSource(flags: [], queue: .global())
        timer?.schedule(wallDeadline: .now() + CGFloat(interval), repeating: .seconds(interval))
        timer?.setEventHandler { [weak self] in
            guard let strongSelf = self else { return }
            strongSelf.enquiryUpdateInfo()
        }
        timer?.resume()
    }
    
    private func enquiryUpdateInfo() {
//        sharedInnerAppFileMgr.fetchRegularUpdateTime()
    }
    
    func stopTimer() {
        timer?.cancel()
        timer = nil
    }
}
