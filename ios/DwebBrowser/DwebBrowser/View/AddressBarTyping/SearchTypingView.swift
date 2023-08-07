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
                .frame(width: screen_width, height: 40)
                .background(Color.bkColor)
            HStack {
                Spacer()
                Button {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    addressBar.inputText = ""
                    addressBar.isFocused = false
                } label: {
                    text
                        .foregroundColor(.dwebTint)
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
        .animation(.easeInOut, value: addressBar.inputText == "")
    }
    
    var text: some View{
        #if DwebBrowser
        Text("取消")
        #else
        Text("取消 C#")
        #endif
    }
}

struct OverlayView_Previews: PreviewProvider {
    static var previews: some View {
        SearchTypingView()
    }
}
