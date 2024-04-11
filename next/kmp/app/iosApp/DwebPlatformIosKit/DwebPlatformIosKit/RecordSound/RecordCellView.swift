//
//  RecordCellView.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/27.
//

import SwiftUI
import AVFoundation
import CoreData

struct RecordCellView: View {
    
    @Environment(\.managedObjectContext) var modelContext
    @EnvironmentObject var environ: EnvironmentModel
    @State private var isPlaying = false
    @State private var currentPlayURLString = ""
    @State private var playingTimeString: Int = 0
    @State private var playedTimeString: Int = 0
    @State private var progress: CGFloat = 0
    @State private var isSlideDrag: Bool = false
    let model: SoundEntity
    @Binding var selectedIndex: Int?
    @Binding var choosedIndex: Int?
    let index: Int
    @Binding var choosedList: [String]
    let multiple: Bool
    let limitCount: Int
    @Binding var showingModal: Bool
    
    var body: some View {
        VStack(alignment: .leading) {
            VStack(alignment: .leading) {
                HStack {
                    Text(model.nameString ?? "")
                        .font(Font.system(size: 16.0, weight: .bold))
                        .padding(.top,8)
                    Spacer()
                    Image(systemName: chooseStatus() ? "checkmark.circle.fill" : "checkmark.circle")
                        .foregroundColor(chooseStatus() ? .blue : .black)
                        .padding(.top, 8)
                        .onTapGesture {
                            
                            if multiple {
                                if choosedList.count >= limitCount {
                                    if model.isSelected {
                                        model.isSelected.toggle()
                                        choosedList.removeAll { oldPath in
                                            return oldPath == model.path
                                        }
                                    } else {
                                        withAnimation {
                                            showingModal = true
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                                            showingModal = false
                                        }
                                    }
                                } else {
                                    model.isSelected.toggle()
                                    if model.isSelected, model.path != nil {
                                        choosedList.append(model.path!)
                                    } else {
                                        choosedList.removeAll { oldPath in
                                            return oldPath == model.path
                                        }
                                    }
                                }
                            } else {
                                choosedIndex = index == choosedIndex ? nil : index
                                choosedList.removeAll()
                                if choosedIndex != nil, model.path != nil {
                                    choosedList.append(model.path!)
                                }
                            }
                        }
                }
                
                HStack {
                    Text(changeStampToDate(stamp: Int(model.timeStamp)))
                        .font(Font.system(size: 12.0))
                    Spacer()
                    if index != selectedIndex {
                        Text(changeSecondToTime(second: Int(model.duration)))
                            .font(Font.system(size: 12.0))
                    }
                }
                .padding(.top,4)
            }
            .background(Color(uiColor: bkColor))
            .onTapGesture {
                selectedIndex = index == selectedIndex ? nil : index
                isPlaying = false
                self.updatePlayingTime(value: 0)
                self.progress = 0
                PlayerManager.shared.stop()
                PlayerManager.shared.initPlayer(url: URL(fileURLWithPath: model.path ?? ""))
            }
            
            if index == selectedIndex {
                CustomSliderView(progress: $progress, totalTime: model.duration, isDrag: $isSlideDrag)
                    .padding(.top,16)
                    .frame(height: 20)
                
                
                HStack {
                    Text(changeSecondToTime(second: playingTimeString))
                        .font(Font.system(size: 12.0))
                    Spacer()
                    Text(changeSecondToTime(second: playedTimeString))
                        .font(Font.system(size: 12.0))
                }
                .padding(.top,8)
                
                HStack {
                    Spacer()
                    
                    Image(systemName: isPlaying ? "pause.circle" : "play.circle")
                        .resizable()
                        .frame(width: 30,height: 30)
                        .onTapGesture {
                            isPlaying = !isPlaying
                            print("Play")
                            if isPlaying {
                                if currentPlayURLString != model.path {
//                                    PlayerManager.shared.initPlayer(url: URL(string: "http://downsc.chinaz.net/files/download/sound1/201206/1638.mp3")!)
                                    currentPlayURLString = model.path ?? ""
                                }
                                if self.playingTimeString == model.duration {
                                    self.progress = 0
                                    PlayerManager.shared.stop()
                                    DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
                                        PlayerManager.shared.initPlayer(url: URL(fileURLWithPath: model.path ?? ""))
                                        PlayerManager.shared.play()
                                    }
                                } else {
                                    PlayerManager.shared.initPlayer(url: URL(fileURLWithPath: model.path ?? ""))
                                    PlayerManager.shared.play()
                                }
                            } else {
                                PlayerManager.shared.pause()
                            }
                        }
                    
                    Spacer()
                    
                    Image(systemName: "trash")
                        .resizable()
                        .frame(width: 25,height: 25)
                        .onTapGesture {
                            selectedIndex = nil
                            PlayerManager.shared.stop()
                            withAnimation {
                                deleteVoiceFile(path: model.path ?? "")
                                modelContext.delete(model)
                                try? modelContext.save()
                                
                            }
                        }
                }
                .padding(.leading,16)
                .onChange(of: PlayerManager.shared.isFinish) { oldValue, newValue in
                    if newValue {
                        isPlaying = false
                    }
                    PlayerManager.shared.isFinish = false
                }
                .onChange(of: PlayerManager.shared.currentPlayTime) { oldValue, newValue in
                    print("oldValue  \(oldValue)   newValue  \(newValue)  duration  \(model.duration)")
                    if currentPlayURLString == model.path {
                        if newValue == 0 {
                            self.updatePlayingTime(value: Int(newValue))
                            if Int(round(oldValue)) == model.duration {
                                self.progress = min(1.0, newValue / Double(model.duration))
                            } else {
                                withAnimation {
                                    self.progress = min(1.0, newValue / Double(model.duration))
                                }
                            }
                        } else if newValue != 0, newValue > oldValue {
                            self.updatePlayingTime(value: Int(newValue))
                            self.progress = min(1.0, newValue / Double(model.duration))
                            if Int(round(newValue)) == model.duration {
                                self.progress = 1.0
                                self.updatePlayingTime(value: Int(model.duration))
                                PlayerManager.shared.pause()
                                isPlaying = false
                            }
                        }
                    }
                }
                .onChange(of: progress) { oldValue, newValue in
                    if isSlideDrag {
                        isPlaying = false
                        PlayerManager.shared.pause()
                        PlayerManager.shared.updateSliderValueChanged(value: Int(newValue * CGFloat(model.duration)))
                        self.updatePlayingTime(value: Int(newValue * CGFloat(model.duration)))
                    }
                }
                .onChange(of: environ.isRecording) { oldValue, newValue in
                    if newValue {
                        isPlaying = false
                        PlayerManager.shared.stop()
                        selectedIndex = nil
                    }
                }
            }
            
            Spacer()
            
        }
        .padding(.horizontal,16)
    }
    
    private func chooseStatus() -> Bool {
        if multiple {
            return model.isSelected
        } else {
            return index == choosedIndex
        }
    }
    
    private func updatePlayingTime(value: Int) {
        self.playingTimeString = value
        self.playedTimeString = Int(model.duration) - value
    }
    
    private func changeStampToDate(stamp: Int) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(stamp))
        let formatter = DateFormatter()
        var tmpString: String = ""
        let distance = dateDistance(date: date)
        if distance == 0 {
            formatter.dateFormat = "HH:mm"
        } else if distance == 1 {
            formatter.dateFormat = "HH:mm"
            tmpString = "昨天 "
        } else {
            formatter.dateFormat = "yyyy-MM-dd HH:mm"
        }
        let dateString = formatter.string(from: date)
        return tmpString + dateString
    }
    
    private func dateDistance(date: Date) -> Int {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        let currentComponents = calendar.dateComponents([.year, .month, .day], from: Date())
        let cmps = calendar.dateComponents([.day], from: components, to: currentComponents)
        return cmps.day ?? -1
    }
    
    private func changeSecondToTime(second: Int) -> String {
        if second > 9 {
            return "0:\(second)"
        }
        return "0:0\(second)"
    }
    
    //删除文件
    private func deleteVoiceFile(path: String) {
        let manager = FileManager.default
        let isExist = manager.fileExists(atPath: path)
        if isExist {
            try? manager.removeItem(atPath: path)
        }
    }
}

