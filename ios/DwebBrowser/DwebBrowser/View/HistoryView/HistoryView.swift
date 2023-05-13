//
//  HistoryView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/4/23.
//

import SwiftUI

//收藏列表类似

struct HistoryView: View {
    @ObservedObject var historyStore = HistoryStore()
    
    var body: some View{
        List{
              
            ForEach(historyStore.history.grouped(by: \.date)){ section in
                Section(header: Text(section.date.formatted(.dateTime)).foregroundColor(.blue)) {
                    ForEach(section.items){ item in
                        VStack(alignment: .leading) {
                            Text(item.title)
                                .font(.headline)
                            Text(item.url)
                                .font(.subheadline)
                                .foregroundColor(.gray)
                        }
                        
                    }
                }
            }
        }
        .listStyle(PlainListStyle())
        .navigationTitle("History")
        
    }
    
}
struct PresentView: View {
    @State private var showingModal = false

    var body: some View {
        Button("Show Modal") {
            showingModal = true
        }
        .sheet(isPresented: $showingModal) {
            HistoryView()
        }
    }
}

struct HistoryView_Previews: PreviewProvider {
    static var previews: some View {
        PresentView()
    }
}
