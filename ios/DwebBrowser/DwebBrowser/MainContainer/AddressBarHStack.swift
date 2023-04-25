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
            AddressBarHStack(adressBarHstackOffset: $testOffset)
            Text("dragOffset:\(testOffset)")
                .background(.green)
        }.background(.red)
    }
}

struct AddressBarHStack: View {
    @State private var currentPage: Int = 0
    @State private var offset: CGFloat = 0
    
    @State private var dragOffset: CGFloat = 0
    @Binding var adressBarHstackOffset: CGFloat
    
    var body: some View {
        GeometryReader { innerGeometry in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    AddressBar(adressBarHstackOffset: $adressBarHstackOffset, inputText: "")
                        .frame(width: screen_width)
                    AddressBar(adressBarHstackOffset: $adressBarHstackOffset, inputText: "")
                        .frame(width: screen_width)
                    AddressBar(adressBarHstackOffset: $adressBarHstackOffset, inputText: "")
                        .frame(width: screen_width)
                }
                .onChange(of: innerGeometry.frame(in: .global).minX) { offsetX in
                    // Do something with the offsetY value
                    print("Offset X: \(offsetX)")
                    adressBarHstackOffset = offsetX
                }
            }
            .onChange(of: innerGeometry.frame(in: .global).minX) { offsetX in
                // Do something with the offsetY value
                print("Offset X: \(offsetX)")
                adressBarHstackOffset = offsetX
            }
            .background(.orange)
        }
    }
}

struct AddressBar: View {
    @Binding var adressBarHstackOffset: CGFloat
    @State var inputText: String = ""
    @FocusState var isAdressBarFocused: Bool
    
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
                        adressBarHstackOffset = offsetX
                        
                    }
                
            }.frame(height: 60)
        }
    }
}


extension View {
    func placeholder<Content: View>(
        when shouldShow: Bool,
        alignment: Alignment = .leading,
        @ViewBuilder placeholder: () -> Content) -> some View {
            
            ZStack(alignment: alignment) {
                placeholder().opacity(shouldShow ? 1 : 0)
                self
            }
        }
}

