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
    @State private var isOverlay: Bool = false
    
    var body: some View {
 
        VStack {
            List {
                ForEach(0..<records.count, id: \.self) { index in
                    let model = records[index]
                    RecordCellView(model: model, selectedIndex: $selectedIndex, index: index)
                        .background(Color(uiColor: bkColor))
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            }
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
        
    }
}

#Preview {
    RecordMainView()
}
