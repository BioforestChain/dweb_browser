//
//  SingleRecordView.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/30.
//

import SwiftUI

struct SingleRecrodView: View {
    
    enum PlayerType {
        case none, playing, pause, finished
    }
    
    @State private var playType: PlayerType = .none
    @State var nameString: String = ""
    @State private var isPlaying: Bool = false
    @State private var isRecording: Bool = false
    @State private var isEndable: Bool = true
    @State private var isPlayEndable: Bool = true
    @State private var isRecordEndable: Bool = false
    private let recordManager = RecordManager.shared
    @State private var timer: Timer?
    @State var timeString: String = "00:00"
    @State private var recordPath: String = ""
    @State private var voiceIndex = 0
    @State var heights: [CGFloat] = Array(repeating: 0, count: Int(UIScreen.main.bounds.width / 4))
    @State var totalHeights: [CGFloat] = []
    private let voiceHeight: CGFloat = 400
    @State private var isClickFinish = false
    
    var body: some View {
        VStack(alignment: .center) {
//            TextField("录音标题", text: $nameString)
            Text("录音")
                .fontWeight(.bold)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .padding(.top, 26)
            
            VoiceAnimationView(heights: $heights)
                .frame(height: voiceHeight)
                .background(.secondary.opacity(0.3))
                .padding(.top,20)
            
            Text(timeString)
                .font(Font.system(size: 36, weight: .bold))
                .padding(.top,16)
            
            Button {
                guard !recordPath.isEmpty else { return }
                if playType == .playing {
                    playType = .pause
                } else {
                    playType = .playing
                }
            } label: {
                Image(systemName: playType == .playing ? "pause.circle" : "play.circle")
                    .resizable()
                    .frame(width: 40,height: 40)
            }
            .padding(.top,16)
            .disabled(isPlayEndable)

            HStack {
                
                Spacer()
                
                Button {
                    resetVoiceData()
                    isRecording = true
                } label: {
                    Image(systemName: "repeat.circle")
                        .resizable()
                        .frame(width: 30,height: 30)
                }
                .disabled(isEndable)
                
                Button {
                    isRecording.toggle()
                } label: {
                    Image(systemName: isRecording ? "pause.circle" : "record.circle")
                        .resizable()
                        .foregroundColor(isRecordEndable ? Color.secondary : .blue)
                        .cornerRadius(15)
                        .frame(width: 40,height: 40)
                }
                .disabled(isRecordEndable)
                .padding(.leading,50)
                
                Button {
                    isClickFinish = true
                    RecordManager.shared.completeSingleRecordCallback?(recordPath)
                } label: {
                    Image(systemName: "checkmark.circle.fill")
                        .resizable()
                        .frame(width: 30,height: 30)
                }
                .padding(.leading,50)
                .disabled(isEndable)
                
                Spacer()
            }
            .padding(.top,30)
            
            Spacer()
        }
        .onChange(of: isRecording) { oldValue, newValue in
            isEndable = newValue
            isPlayEndable = newValue
            if newValue {
                resetVoiceData()
                recordManager.startRecorder()
                startTimer()
            } else {
                recordManager.stopRecorder()
                stopTimer()
            }
        }
        .onChange(of: playType) { oldValue, newValue in
            guard !recordPath.isEmpty else { return }
            if newValue == .playing {
                isEndable = true
                isRecordEndable = true
            } else {
                isEndable = false
                isRecordEndable = false
            }
            
            if newValue == .playing {
                if oldValue != .pause {
                    self.heights = [CGFloat](self.totalHeights[0..<self.heights.count])
                    self.voiceIndex = self.heights.count
                    PlayerManager.shared.initPlayer(url: URL(fileURLWithPath: recordPath))
                }
                PlayerManager.shared.play()
            } else {
                PlayerManager.shared.pause()
            }
        }
        .onChange(of: recordManager.path) { oldValue, newValue in
            if !newValue.isEmpty {
                recordPath = newValue
            }
        }
        .onChange(of: PlayerManager.shared.currentPlayTime) { oldValue, newValue in
            if Int(round(newValue)) <= recordManager.record_duration {
                self.timeString = handleTimeFormattor(time: Int(round(newValue)))
                if voiceIndex < self.totalHeights.count - 1 {
                    voiceIndex += 1
                    self.updateVoiceValue(value: self.totalHeights[voiceIndex])
                }
            }
        }
        .onChange(of: PlayerManager.shared.isFinish, { oldValue, newValue in
            if newValue {
                playType = .finished
                PlayerManager.shared.stop()
            }
            PlayerManager.shared.isFinish = false
        })
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification), perform: { _ in
            
            isRecording = false
        })
        .onAppear {
            totalHeights = heights
        }
        .onDisappear {
            timer?.invalidate()
            timer = nil
            recordManager.stopRecorder()
            PlayerManager.shared.stop()
            recordManager.removeNotification()
            guard !isClickFinish else { return }
            RecordManager.shared.completeSingleRecordCallback?("")
        }
    }
    
    private func startTimer() {
        let startStamp = Date().timeStamp
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true, block: { _ in
            let meter = self.recordManager.voiceMeters()
            self.updateVoiceValue(value: CGFloat(meter))
            let endStamp = Date().timeStamp
            let distance = endStamp - startStamp
            if distance > 59 {
                self.stopTimer()
            } else {
                self.timeString = handleTimeFormattor(time: distance)
            }
        })
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
        self.isRecording = false
        isEndable = false
    }
    
    private func updateVoiceValue(value: CGFloat) {
        heights.removeFirst()
        heights.append(value * (voiceHeight - 200))
        if isRecording {
            totalHeights.append(value)
        }
    }
    
    private func handleTimeFormattor(time: Int?) -> String {
        guard time != nil else { return "00"}
        if time! > 9 {
            return "00:\(time!)"
        }
        return "00:0\(time!)"
    }
    
    private func resetVoiceData() {
        heights = Array(repeating: 0, count: Int(UIScreen.main.bounds.width / 4))
        totalHeights = heights
        self.timeString = "00:00"
    }
}

#Preview {
    SingleRecrodView(nameString: "")
}

