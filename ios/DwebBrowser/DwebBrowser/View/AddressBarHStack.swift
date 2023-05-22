//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import SwiftUI

struct AddressBarHStack: View {
    @EnvironmentObject var states: ToolbarState
    @EnvironmentObject var browser: BrowerVM
    
    @State var currentIndex: Int = 0
    @State var offsetX: CGFloat = 0
    
    var body: some View {
        GeometryReader { innerGeometry in
            PagingScroll(contentSize: browser.pages.count, content: AddressBarHContainer(), currentPage: $currentIndex, offsetX: $offsetX)
                .onChange(of: currentIndex) { newValue in
                    browser.selectedTabIndex = currentIndex
                    //                    print("currentIndex changed to \(newValue)")
                }
                .onChange(of: offsetX) { newValue in
                    browser.addressBarOffset = offsetX
                    //                    print("currentIndex offsetX to \(offsetX)")
                }
        }
        .frame(height: browser.addressBarHeight)
    }
}

struct AddressBarHContainer:View{
    @EnvironmentObject var browser: BrowerVM
    
    var body: some View{
        HStack(spacing: 0) {
            ForEach(browser.pages){ page in
                AddressBar(inputText: "", webStore: page.webWrapper)
                    .frame(width: screen_width)
            }
        }
        .background(.white)
    }
}

struct AddressBar: View {
    @State var inputText: String = ""
    @FocusState var isAdressBarFocused: Bool
    
    @ObservedObject var webStore: WebWrapper
    
    var body: some View {
        GeometryReader{ geometry in
            
            ZStack() {
                Color(.white)
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(.darkGray))
                    .frame(width:screen_width - 48 ,height: 40)
                    .overlay {
                        GeometryReader { geometry in
                            VStack(alignment: .leading, spacing: 0) {
                                ProgressView(value: webStore.estimatedProgress)
                                    .progressViewStyle(LinearProgressViewStyle())
                                    .foregroundColor(.blue)
                                    .background(Color(white: 1))
                                    .cornerRadius(4)
                                    .frame(height: webStore.estimatedProgress >= 1.0 ? 0 : 3)
                                    .alignmentGuide(.leading) { d in
                                        d[.leading]
                                    }
                                    .opacity(webStore.estimatedProgress > 0.0 && webStore.estimatedProgress < 1.0 ? 1 : 0)
                                
                            }
                            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
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
            }.frame(height: addressBarH)
        }
    }
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        AddressBar(webStore: WebWrapper(webCache: WebCache()))
            .environmentObject(BrowerVM())
    }
}
