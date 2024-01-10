//
//  OverlayView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI

struct SearchTypingView: View {
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var dragScale: WndDragScale

    var body: some View {
        VStack(spacing: 0) {
            Text("搜索")
                .font(.system(size: dragScale.scaledFontSize(maxSize: 20), weight: .heavy))
                .padding(.vertical, 6)

            HStack {
                Spacer()
                Button(action: releaseFocuse) {
                    Text("取消")
                        .font(.system(size: dragScale.scaledFontSize(maxSize: 18)))
                        .foregroundColor(Color.dwebTint)
                }
                .padding(.trailing, 20)
            }
            
            SearchResultView()
        }
        .background(Color.bkColor)

        .onDisappear {
            addressBar.inputText = ""
            addressBar.searchInputText = ""
            addressBar.isFocused = false
            enterType = .none
        }
        .animation(.easeInOut, value: addressBar.inputText == "")
    }

    func releaseFocuse() {
//        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
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
