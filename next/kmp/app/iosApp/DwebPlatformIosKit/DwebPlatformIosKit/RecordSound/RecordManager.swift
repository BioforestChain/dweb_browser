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
    var completeCallback: ((String) -> Void)?
    var completeSingleRecordCallback: ((String) -> Void)?
    
    override init() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord)
        try? session.setActive(true)
        
        recorderSettings = [AVFormatIDKey: kAudioFormatMPEG4AAC,
                    AVNumberOfChannelsKey: 2,
                 AVEncoderAudioQualityKey: AVAudioQuality.max.rawValue,
                      AVEncoderBitRateKey: 320000,
                          AVSampleRateKey: 44100.0]
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
        let averageV = recorder?.averagePower(forChannel: 0)
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
        completeCallback?(path)
    }
}
