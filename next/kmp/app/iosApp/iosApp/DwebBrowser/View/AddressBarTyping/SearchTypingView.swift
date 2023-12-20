//
//  OverlayView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/10.
//

import SwiftUI

struct SearchTypingView: View {
    @EnvironmentObject var addressBar: AddressBarState

    var body: some View {
        VStack(spacing: 0) {
            Text("搜索")
                .font(.headline)
                .frame(height: 40)
                .frame(maxWidth: .infinity)
                .background(Color.bkColor)

            HStack {
                Spacer()
                Button(action: releaseFocuse) {
                    Text("取消")
                        .foregroundColor(Color.dwebTint)
                        .padding(8)
                }
                .padding(.trailing, 20)
            }
            .background(Color.bkColor)

            if addressBar.inputText == "" && !InstalledAppMgr.shared.apps.isEmpty {
                VStack {
                    InnerAppGridView()
                    Spacer()
                }
            } else {
                SearchResultView()
                    .background(Color.bkColor)
            }
        }
        .onDisappear {
            addressBar.inputText = ""
            addressBar.searchInputText = ""
            addressBar.isFocused = false
        }
        .animation(.easeInOut, value: addressBar.inputText == "")
    }

    func releaseFocuse() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
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
