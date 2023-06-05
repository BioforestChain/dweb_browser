//
//  OverlayMaskView.swift
//  DwebBrowser
//
//  Created by ui06 on 5/30/23.
//

import SwiftUI

struct OverlayMaskView: View {
    @Binding var isEditing: Bool
    var body: some View {
        if isEditing {
            VStack{
                Color.black.opacity(0.7)
                    .edgesIgnoringSafeArea(.all)
                    .onTapGesture {
                        isEditing = false
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    }
                    .overlay(alignment: .top) {
                        Color.white
                            .edgesIgnoringSafeArea(.all)
                            .frame(height: 40)
                            .overlay(alignment: .bottomTrailing) {
                                Button {
                                    isEditing = false
                                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                                    
                                } label: {
                                    Text("取消")
                                        .font(.system(size: 18))
                                }
                                .padding(10)
                            }
                    }
            }
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
                OverlayMaskView(isEditing: Binding(get: { isAdressBarFocused }, set: { isAdressBarFocused = $0 }))
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
