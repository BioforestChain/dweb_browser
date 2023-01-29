//
//  RecordView.swift
//  DWebBrowser
//
//  Created by mac on 2022/6/14.
//

import UIKit
import AVFoundation

class RecordView: UIView {

    private var aacPath: String = ""
    private var recorderSetingsDic: [String: Any]?
    private var recorder: AVAudioRecorder?
    private var player: AVAudioPlayer?
    private var volumeTimer: Timer!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord)
        try? session.setActive(true)
        
        aacPath = documentdir + "/play.aac"

        recorderSetingsDic = [
            AVFormatIDKey: NSNumber(value: kAudioFormatMPEG4AAC),
            AVNumberOfChannelsKey: 2, //录音的声道数，立体声为双声道
            AVEncoderAudioQualityKey : AVAudioQuality.max.rawValue,
            AVEncoderBitRateKey : 320000,
            AVSampleRateKey : 44100.0 //录音器每秒采集的录音样本数
        ]
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    //开始录音
    func startRecord() {
        
        guard let url = URL(string: aacPath) else { return }
        recorder = try? AVAudioRecorder(url: url, settings: recorderSetingsDic!)
        recorder?.isMeteringEnabled = true
        recorder?.prepareToRecord()
        recorder?.record()
        
        volumeTimer = Timer.scheduledTimer(timeInterval: 0.1, target: self, selector: #selector(levelTimer), userInfo: nil, repeats: true)
    }
    //结束录音
    func endAction() {
        recorder?.stop()
        recorder = nil
        volumeTimer.invalidate()
        volumeTimer = nil
        //结束动画
    }
    //开始播放录音
    func playAction() {
        guard let url = URL(string: aacPath) else { return }
        player = try? AVAudioPlayer(contentsOf: url)
        //默认声音是从听筒位置发出, 如果嫌音量小的话，可以将声音输出设置为下面的扬声器。
        try? AVAudioSession.sharedInstance().overrideOutputAudioPort(.speaker)
        player?.play()
    }
    //定时器，根据音量大小做动画
    @objc private func levelTimer() {
        recorder?.updateMeters()  // 刷新音量数据
        let average: Float = recorder?.averagePower(forChannel: 0) ?? 0  //获取音量的平均值
        let max = recorder?.peakPower(forChannel: 0) ?? 0  //获取音量最大值
        let lowPassResult = pow(Double(10), Double(0.05 * max))
        //音频动画
    }
}
