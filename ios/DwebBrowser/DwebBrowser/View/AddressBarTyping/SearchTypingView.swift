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
        NavigationView {
            ZStack{
                if addressBar.inputText == ""{
                    VStack{
                        InnerAppGridView()
                        Spacer()
                    }
                }else{
                    SearchResultView()
                }
            }
            .animation(.easeInOut, value: addressBar.inputText == "")
            .navigationBarTitleDisplayMode(.inline)
            .navigationTitle("搜索")
            
            .toolbar {
                Button(action: {
                    print(" release the first responder.")
                    addressBar.inputText = ""
                    addressBar.isFocused = false
                }) {
                    Text("取消")
                        .foregroundColor(.dwebTint)
                        .padding(8)
                }
            }
        }
    }
}

struct OverlayView_Previews: PreviewProvider {
    static var previews: some View {
        SearchTypingView()
    }
}



