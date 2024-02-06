//
//  OverlayView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI

struct SearchTypingView: View {
    @EnvironmentObject var addressBar: AddressBarState
    @Environment(WndDragScale.self) var dragScale

    var body: some View {
        VStack(spacing: 0) {
            Text("搜索")
                .font(dragScale.scaledFont_18)
                .fontWeight(.heavy)
                .padding(.vertical, 6)

            HStack {
                Spacer()
                Button(action: releaseFocuse) {
                    Text("取消")
                        .font(dragScale.scaledFont_18)
                        .foregroundColor(Color.primary)
                }
                .padding(.trailing, 20)
            }
            
            SearchResultView()
        }
        .background(Color.bk)

        .onDisappear {
            addressBar.inputText = ""
            addressBar.searchInputText = ""
            addressBar.isFocused = false
            enterType = .none
        }
        .animation(.easeInOut, value: addressBar.inputText == "")
    }

    func releaseFocuse() {
        addressBar.inputText = ""
        addressBar.searchInputText = ""
        addressBar.isFocused = false
    }
}

struct OverlayView_Previews: PreviewProvider {
    static var previews: some View {
        SearchTypingView()
    }
}
