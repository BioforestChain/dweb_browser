//
//  PlayerHandler.swift
//  55
//
//  Created by ios on 2024/1/26.
//

import Foundation
import AVFoundation

@Observable
class PlayerManager {
    
    static let shared = PlayerManager()
    private var playerItem: AVPlayerItem?
    private var player: AVPlayer?
    var currentPlayTime: Double = 0
    var isFinish: Bool = false
    
    func initPlayer(url: URL) {
        
        guard player == nil else { return }
        playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        let interval = CMTime(seconds: 0.016,
                              preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player!.addPeriodicTimeObserver(forInterval: interval, queue: .main) { cmTime in
            if self.player?.currentItem?.status == .readyToPlay && self.player?.rate != 0 {
                let currentTime = CMTimeGetSeconds(self.player!.currentTime())
                self.currentPlayTime = self.roundToPlaces(value: currentTime, places: 1)
            }
        }
        
        addAudioNotification()
    }
    
    private func addAudioNotification() {
        NotificationCenter.default.addObserver(self, selector: #selector(finishPlayer(noti:)), name: NSNotification.Name.AVPlayerItemDidPlayToEndTime, object: playerItem)
        
//        NotificationCenter.default.addObserver(self, selector: #selector(handleInterruption(noti:)), name: AVAudioSession.interruptionNotification, object: AVAudioSession.sharedInstance())
       
//        NotificationCenter.default.addObserver(self, selector: #selector(handleRouteChange(noti:)), name: AVAudioSession.routeChangeNotification, object: AVAudioSession.sharedInstance())
    }
    
    func removeNotification() {
        NotificationCenter.default.removeObserver(self)
    }
    
    //保留小数点位数
    func roundToPlaces(value:Double, places:Int) -> Double {
        let divisor = pow(10.0, Double(places))
        return round(value * divisor) / divisor
    }
    
    func play() {
        player?.play()
        
    }
    
    func pause() {
        player?.pause()
    }
    
    func stop() {
        player = nil
    }
    //滑动条变动时，更新播放时间
    func updateSliderValueChanged(value: Int) {
        let targetTime = CMTimeMake(value: Int64(value), timescale: 1)
        player?.seek(to: targetTime)
        if player?.rate == 0 {
//            player?.play()
        }
    }
}

extension PlayerManager {
    
    //结束播放
    @objc private func finishPlayer(noti: Notification) {
        self.isFinish = true
    }
    
    //系统中断响应通知
    @objc private func handleInterruption(noti: Notification) {
        guard let userInfo = noti.userInfo,
            let typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? UInt,
            let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }
        
        switch type {
        case .began:
            print("play begin")
            player?.pause()
        case .ended:
            print("play end")
            try? AVAudioSession.sharedInstance().setActive(true)
            if let optionsValue = userInfo[AVAudioSessionInterruptionOptionKey] as? UInt {
                let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                if options.contains(.shouldResume) {
                    // Interruption ends. Resume playback.
                    player?.play()
                } else {
                    // Interruption ends. Don't resume playback.
                }
            }
        default:
            print("unknown type: \(type)")
        }
    }
    
    //响应音频路由变化
    @objc private func handleRouteChange(noti: Notification) {
        guard let userInfo = noti.userInfo,
              let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else {
            return
        }
        
        // Switch over the route change reason.
        switch reason {
        case .newDeviceAvailable: // New device found.
            print("unknown type: \(reason)")
        case .oldDeviceUnavailable: // Old device removed.
            print("unknown type: \(reason)")
        default: ()
        }
    }
}
