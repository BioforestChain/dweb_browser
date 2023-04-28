//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import SwiftUI

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        WrapTestView()
    }
}


struct WrapTestView:View{
    @State var testOffset: CGFloat = 0
    
    var body: some View{
        VStack{
            Spacer()
            AddressBarHStack()
            Text("dragOffset:\(testOffset)")
                .background(.green)
        }.background(.red)
    }
}

struct AddressBarHStack: View {
    @EnvironmentObject var states: ToolbarState
    @EnvironmentObject var mainVstate: MainViewState

    var body: some View {
        GeometryReader { innerGeometry in
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    AddressBar(inputText: "")
                        .frame(width: screen_width)
                    AddressBar(inputText: "")
                        .frame(width: screen_width)
                    AddressBar(inputText: "")
                        .frame(width: screen_width)
                }
            }
            .onAppear{
                UIScrollView.appearance().isPagingEnabled = true
            }
            .background(.orange)
        }
        .frame(height: states.showMenu ? 0 : addressBarHeight)

        .animation(.easeInOut(duration: 0.3), value: states.showMenu)

    }
}

struct AddressBar: View {
    @State var inputText: String = ""
    @FocusState var isAdressBarFocused: Bool
    @EnvironmentObject var offsetState: MainViewState

    var body: some View {
        GeometryReader{ geometry in
            
            ZStack{
                Color(.white)
                RoundedRectangle(cornerRadius: 10).fill(Color(.darkGray))
                    .frame(width:screen_width - 48 ,height: 40)
                TextField("", text: $inputText)
                
                    .placeholder(when: inputText.isEmpty) {
                        Text("请输入搜索内容").foregroundColor(Color(white: 0.8))
                    }
                    .background(Color(.darkGray))
                    .foregroundColor(.white)
                    .padding(.horizontal,30)
                    .zIndex(1)
                    .keyboardType(.webSearch)
                    .focused($isAdressBarFocused)
                    .onAppear{
                        print(inputText)
                    }
                    .onTapGesture {
                        print("tapped")
                        isAdressBarFocused = true
                    }
                    .onChange(of: geometry.frame(in: .named("Root")).minX) { offsetX in
                        // Do something with the offsetY value
                        print("Offset X: \(offsetX)")
                        offsetState.adressBarHstackOffset = offsetX
                      }
                
            }.frame(height: addressBarHeight)
        }
    }
}

