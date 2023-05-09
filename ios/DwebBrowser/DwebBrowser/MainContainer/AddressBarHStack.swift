//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import SwiftUI

struct AddressBarHContainer:View{
    var body: some View{
        HStack(spacing: 0) {
            AddressBar(inputText: "")
                .frame(width: screen_width)
            AddressBar(inputText: "")
                .frame(width: screen_width)
            AddressBar(inputText: "")
                .frame(width: screen_width)
        }
        .background(.white)
    }
}

struct AddressBarHStack: View {
    @EnvironmentObject var states: ToolbarState
    @EnvironmentObject var browser: BrowerVM
    
    @State private var selectedTab = 0
    
    var body: some View {
        GeometryReader { innerGeometry in
            PageScroll(contentSize: browser.pages.count, content:AddressBarHContainer())
        }
        .frame(height: browser.addressBarHeight)
        .animation(.easeInOut(duration:0.3), value: browser.addressBarHeight)
    }
}

struct AddressBar: View {
    @State var inputText: String = ""
    @FocusState var isAdressBarFocused: Bool
    @EnvironmentObject var browser: BrowerVM
    @State var progressValue: Float = 0.0
    var body: some View {
        GeometryReader{ geometry in
            
            ZStack() {
                Color(.white)
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(.darkGray))
                    .frame(width:screen_width - 48 ,height: 40)
                    .overlay {
                        if progressValue > 0.0 && progressValue <= 1.0{
                            GeometryReader { geometry in
                                VStack(alignment: .leading, spacing: 0) {
                                    
                                    ProgressView(value: progressValue)
                                        .progressViewStyle(LinearProgressViewStyle())
                                        .foregroundColor(.blue)
                                        .background(Color(white: 1))
                                        .cornerRadius(4)
                                        .alignmentGuide(.leading) { d in
                                            d[.leading]
                                        }
                                    
                                        .onDisappear{
                                            
                                        }
                                }
                                .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                
                            }
                        }
                    }
                    .onAppear{
//                        performNetworkRequest()
                    }
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
                        browser.addressBarOffset = offsetX
                    }
            }.frame(height: browser.addressBarHeight)
        }
    }
    
    func performNetworkRequest() {
        // i
        Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { timer in
            if progressValue >= 1.0 {
                timer.invalidate()
            }
            progressValue += 0.05
        }
    }
}


struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        
        AddressBar()
            .environmentObject(BrowerVM())
    }
}
