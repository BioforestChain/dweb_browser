//
//  OverlayMaskView.swift
//  DwebBrowser
//
//  Created by ui06 on 5/30/23.
//

import SwiftUI

struct KeyBoardShowingView: View {
    @Binding var isFocused: Bool
    var body: some View {
        if isFocused {
            SearchTypingView()
                .background(.white)
        }
        
    }
}

struct OverlayMaskTest: View {
    @State var inputText: String = ""
    @FocusState var isAdressBarFocused: Bool
    
    var body: some View {
        VStack{
            ZStack{
                VStack{
                    Color.red
                    Color.blue
                }
                KeyBoardShowingView(isFocused: .constant(false))
            }
            TextField("input something!", text: $inputText)
                .focused($isAdressBarFocused)
                .frame(height: 40)
                .background(.gray)
        }
    }
}

struct OverlayMaskView_Previews: PreviewProvider {
    static var previews: some View {
        OverlayMaskTest()
    }
}
