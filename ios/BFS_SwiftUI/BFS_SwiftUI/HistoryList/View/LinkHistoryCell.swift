//
//  LinkHistoryCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/19.
//

import SwiftUI

enum HistoryOperate {
    case selected
    case unSelected
}

struct LinkHistoryCell: View {
    
    var linkRecord: LinkRecord
    @Binding var deleteList: [String]
    @State private var isSelected = false
    
    var body: some View {
        
        HStack {
            
            Button {
                
            } label: {
                Image(isSelected ? "ico_checkbox_checked" : "ico_checkbox_uncheck")
                    .resizable()
            }
            .frame(width: 20, height: 20)
            .gesture(TapGesture().onEnded({ _ in
                isSelected.toggle()
                if let index = deleteList.firstIndex(of: linkRecord.id.uuidString) {
                    deleteList.remove(at: index)
                } else {
                    deleteList.append(linkRecord.id.uuidString)
                }
            }))
            
            Spacer().frame(width: 16)
            
            Image("ico_bottomtab_book_normal")
                .resizable()
                .frame(width: 30, height: 30)
                .cornerRadius(4)
            
            Spacer().frame(width: 16)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(linkRecord.title)
                    .font(.system(size: 16))
                    .foregroundColor(.black)
                    .frame(height: 25)
                    .lineLimit(1)
                
                Text(linkRecord.link)
                    .font(.system(size: 13))
                    .foregroundColor(.init(white: 0.667))
                    .frame(height: 20)
                    .lineLimit(1)
            }
        }
        .padding(EdgeInsets(top: 15, leading: 15, bottom: 10, trailing: 30))
        .onTapGesture {
            print("click cell")
        }
    }
}

