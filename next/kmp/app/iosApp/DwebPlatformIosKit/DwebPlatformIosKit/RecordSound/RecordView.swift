//
//  RecordView.swift
//  55
//
//  Created by ios on 2024/1/24.
//

import SwiftUI
import CoreData

struct RecordView: View {
    
    @Environment(\.managedObjectContext) var modelContext
    @State var voiceNumber = 0
    private let recordManager = RecordManager.shared
    @State private var timer: Timer?
    @State var timeString: String = "00:00"
    @State private var isRecording = false
    @State private var recordDuration: Int = 0
    @EnvironmentObject var environ: EnvironmentModel
    @State var soundHeights: [CGFloat] = Array(repeating: 0, count: Int(UIScreen.main.bounds.width / 4))
    
    var body: some View {
        VStack(alignment: .center) {
            if isRecording {
                Text(timeString)
                
                VoiceAnimationView(heights: $soundHeights)
                    .frame(height: 40)
            }
            Button(action: {
                isRecording.toggle()
            }, label: {
                ZStack(alignment: .center) {
                    Color.white
                        .cornerRadius(30)
                        .overlay(
                            RoundedRectangle(cornerRadius: 30, style: .continuous)
                                .stroke(.blue, lineWidth: 4.0)
                        )
                        .frame(width: 60,height: 60)
                    
                    Color.red
                        .cornerRadius(isRecording ? 4 : 23)
                        .frame(width: isRecording ? 25 : 46,height: isRecording ? 25 : 46)
                }
            })
            .padding(.top,16)
        }
        .onChange(of: isRecording, { oldValue, newValue in
            environ.isRecording = newValue
            if newValue {
                recordManager.startRecorder()
                startTimer()
            } else {
                recordManager.stopRecorder()
                stopTimer()
            }
        })
        .onChange(of: recordManager.record_duration, { oldValue, newValue in
            if newValue > 0 {
                recordDuration = Int(newValue)
            }
        })
        .onChange(of: recordManager.path) { oldValue, newValue in
            if !newValue.isEmpty {
                updateRecordData(duration: recordDuration, path: newValue)
            }
        }
        .onChange(of: recordManager.isRecording) { oldValue, newValue in
            isRecording = newValue
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification), perform: { _ in
            print("didEnterBackgroundNotification")
            //TODO 进入后台
            recordManager.disappearType = .enterBackground
            isRecording = false
        })
        .onDisappear {
            //TODO 界面消失后的操作
            recordManager.stopRecorder()
            stopTimer()
            recordManager.removeNotification()
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
        self.timeString = "00:00"
        self.voiceNumber = 0
    }
    
    private func updateRecordData(duration: Int, path: String) {
        recordsCount += 1
        withAnimation {
            let model = SoundEntity(context: modelContext)
            model.nameString = "标准\(recordsCount)"
            model.timeStamp = Int64(Date().timeStamp)
            model.duration = Int64(duration)
            model.path = path
            try? modelContext.save()
        }
    }
    
    private func updateVoiceValue(value: CGFloat) {
        soundHeights.removeFirst()
        soundHeights.append(value * 30)
    }
    
    private func handleTimeFormattor(time: Int?) -> String {
        guard time != nil else { return "00"}
        if time! > 9 {
            return "00:\(time!)"
        }
        return "00:0\(time!)"
    }
}

#Preview {
    RecordView()
}
