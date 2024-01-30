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
        
        NotificationCenter.default.addObserver(self, selector: #selector(finishPlayer(noti:)), name: NSNotification.Name.AVPlayerItemDidPlayToEndTime, object: playerItem)
    }
    
    //保留小数点位数
    func roundToPlaces(value:Double, places:Int) -> Double {
        let divisor = pow(10.0, Double(places))
        return round(value * divisor) / divisor
    }
    
    func removeNotification() {
        NotificationCenter.default.removeObserver(self)
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
    
    //结束播放
    @objc private func finishPlayer(noti: Notification) {
        self.isFinish = true
    }
}
