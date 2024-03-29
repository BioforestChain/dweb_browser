//
//  RecordManager.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/23.
//

import Foundation
import AVFoundation

@Observable
class RecordManager: NSObject {
    
    enum RecordDisappearType {
        case clickRecord
        case enterBackground
    }
    
    static let shared = RecordManager()
    private var recorder: AVAudioRecorder?
    private var recorderSettings: [String:Any] = [:]
    
    private var recordPath: String {
        get {
            let docDir = createDocument()
            let stamp = Date().timeStamp
            return docDir + "/\(stamp).aac"
        }
    }
    
    var path: String = ""
    var record_duration: Int = 0
    var isRecording: Bool = false
    var disappearType: RecordDisappearType = .clickRecord
    var completeCallback: ((String) -> Void)?
    var completeSingleRecordCallback: ((String) -> Void)?
    
    override init() {
        super.init()
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord)
        try? session.setActive(true)
        
        recorderSettings = [AVFormatIDKey: kAudioFormatMPEG4AAC,
                    AVNumberOfChannelsKey: 2,
                 AVEncoderAudioQualityKey: AVAudioQuality.max.rawValue,
                      AVEncoderBitRateKey: 320000,
                          AVSampleRateKey: 44100.0]
        
        self.addAudioNotification()
    }
    
    private func addAudioNotification() {
//        NotificationCenter.default.addObserver(self, selector: #selector(handleInterruption(noti:)), name: AVAudioSession.interruptionNotification, object: AVAudioSession.sharedInstance())
//        
//        NotificationCenter.default.addObserver(self, selector: #selector(handleRouteChange(noti:)), name: AVAudioSession.routeChangeNotification, object: AVAudioSession.sharedInstance())
    }
    
    func removeNotification() {
        NotificationCenter.default.removeObserver(self)
    }
    
    func startRecorder() {
        if let recorder = recorder, recorder.isRecording {
            return
        }
        
        guard let url = URL(string: recordPath) else { return }
        recorder = try? AVAudioRecorder(url: url, settings: recorderSettings)
        recorder?.isMeteringEnabled = true
        recorder?.prepareToRecord()
        recorder?.record()
        recorder?.delegate = self
    }
    
    func stopRecorder() {
        record_duration = min(Int(round(recorder?.currentTime ?? 0)), 59)
        recorder?.stop()
        recorder = nil
    }
    
    func voiceMeters() -> Float {
        recorder?.updateMeters()
        recorder?.averagePower(forChannel: 0)
        let maxV = recorder?.peakPower(forChannel: 0) ?? 0
        return pow(10.0, maxV * 0.05)
    }
    
    //创建文件夹
    func createDocument() -> String {
        
        let manager = FileManager.default
        let docDir = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first ?? ""
        let dirPath = docDir + "/soundVoice"
        let isExist = manager.fileExists(atPath: dirPath)
        if !isExist {
            try? manager.createDirectory(atPath: dirPath, withIntermediateDirectories: true)
        }
        return dirPath
    }
}

extension RecordManager: AVAudioRecorderDelegate {
  
    func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder, successfully flag: Bool) {
        path = recorder.url.absoluteString
        if disappearType == .clickRecord {
            completeCallback?(path)
        }
    }
}

extension RecordManager {
    
    //系统中断响应通知
    @objc private func handleInterruption(noti: Notification) {
        guard let userInfo = noti.userInfo,
            let typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? UInt,
            let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }
        
        switch type {
        case .began:
            if let recorder = recorder, recorder.isRecording {
                //停止录音
                print("began")
            }
        case .ended:
            print("ended")
            try? AVAudioSession.sharedInstance().setActive(true)
            if let optionsValue = userInfo[AVAudioSessionInterruptionOptionKey] as? UInt {
                let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                if options.contains(.shouldResume) {
                    // Interruption ends. Resume playback.
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
            //暂停录音
            isRecording = false
        case .oldDeviceUnavailable: // Old device removed.
            isRecording = false
        default: ()
        }
    }
}
