//
//  RecordMainView.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/27.
//

import SwiftUI
import CoreData


struct RecordMainView: View {
    
    @FetchRequest(sortDescriptors: [NSSortDescriptor(key: "timeStamp", ascending: true)]) var records: FetchedResults<SoundEntity>
    @StateObject var environ = EnvironmentModel()
    @State private var selectedIndex: Int? = nil
    @State private var choosedIndex: Int? = nil
    @State private var isOverlay: Bool = false
    @State private var isClickSend = false
    @State private var pathList: [String] = []
    @State private var showingModal: Bool = false
    
    var multiple: Bool
    var limit: Int
    
    var body: some View {
        ZStack {
            VStack {
                
                HStack {
                    Spacer()
                    Text("录音列表")
                    Spacer()
                    Button(action: {
                        //                    guard let index = choosedIndex else { return }
                        //                    isClickSend = true
                        //                    let model = records[index]
                        //                    RecordManager.shared.completeCallback?(model.path ?? "")
                        guard pathList.count > 0 else { return }
                        isClickSend = true
                        let joinString = pathList.joined(separator: ",")
                        RecordManager.shared.completeCallback?(joinString)
                    }, label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .resizable()
                            .frame(width: 25, height: 25)
                            .disabled(pathList.count == 0)
                        //                        .disabled(choosedIndex == nil)
                        
                    })
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                
                Rectangle()
                    .fill(Color.secondary.opacity(0.2))
                    .frame(height: 0.5)
                
                List {
                    ForEach(0..<records.count, id: \.self) { index in
                        let model = records[index]
                        RecordCellView(model: model, selectedIndex: $selectedIndex, choosedIndex: $choosedIndex, index: index, choosedList: $pathList, multiple: multiple, limitCount: limit, showingModal: $showingModal)
                            .background(Color(uiColor: bkColor))
                    }
                    .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                }
                .listStyle(.plain)
                .onChange(of: environ.isRecording, { oldValue, newValue in
                    self.isOverlay = newValue
                })
                .overlay {
                    if isOverlay {
                        Color.primary
                            .opacity(0.1)
                    }
                }
                
                RecordView()
            }
            .environmentObject(environ)
            .onAppear(perform: {
                recordsCount = records.count
            })
            .onDisappear {
                PlayerManager.shared.stop()
                pathList.removeAll()
                for model in records {
                    if model.isSelected {
                        model.isSelected = false
                    }
                }
                guard !isClickSend else { return }
                RecordManager.shared.completeCallback?("")
            }
            
            if showingModal {
                Text("请注意，最多选择2条录音")
                    .frame(width: 300, height: 60)
                    .background(.white)
                    .cornerRadius(20)
                    .shadow(radius: 10)
                    .transition(.scale)
            }
        }
    }
}

//#Preview {
//    RecordMainView()
//}
