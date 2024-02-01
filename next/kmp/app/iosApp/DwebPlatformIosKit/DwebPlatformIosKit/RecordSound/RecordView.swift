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
    @State private var counter = 0
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
                isRecording = !isRecording
                environ.isRecording = isRecording
                if isRecording {
                    recordManager.startRecorder()
                    startTimer()
                } else {
                    recordManager.stopRecorder()
                    stopTimer()
                }
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
    }
    
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true, block: { _ in
            let meter = self.recordManager.voiceMeters()
            self.updateVoiceValue(value: CGFloat(meter))
            self.counter += 1
            if self.counter > 119 {
                self.stopTimer()
            } else {
                if self.counter % 2 == 0 {
                    self.timeString = handleTimeFormattor(time: counter / 2)
                }
            }
        })
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
        self.counter = 0
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
