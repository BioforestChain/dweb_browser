//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import SwiftUI

struct AddressBarHStack: View {
    @EnvironmentObject var tabState: TabState
    @EnvironmentObject var addrBarOffset: AddrBarOffset

    @Binding var selectedTabIndex: Int
//    @State var scrolltoIndex: Int = 0
//    @ObservedObject var scrollXoffset: CGFloat

    var body: some View {
        PagingScroll(contentSize: WebWrapperMgr.shared.store.count, content: AddressBarHContainer(), currentPage: $selectedTabIndex, offsetX: $addrBarOffset.onX)
//            .onChange(of: selectedTabIndex) { newValue in
//                addrBarOffset.onX = -screen_width * CGFloat( selectedTabIndex)
//            }
//            .onChange(of: scrollXoffset) { newValue in
//                xoffset.offset = scrollXoffset
//            }
//            .offset(x: xoffset.offset)
            .frame(height: tabState.addressBarHeight)
    }
}

struct AddressBarHContainer:View{
    var body: some View{
        HStack(spacing: 0) {
            ForEach(WebWrapperMgr.shared.store){ webWrapper in
                AddressBar(inputText: "", webWrapper: webWrapper)
                    .frame(width: screen_width)
            }
        }
        .background(.white)
    }
}

struct AddressBar: View {
    @State var inputText: String = "inputText is empty"
    @FocusState var isAdressBarFocused: Bool
    
    @ObservedObject var webWrapper: WebWrapper
    
    var body: some View {
        GeometryReader{ geometry in
            
            ZStack() {
                Color(.white)
                RoundedRectangle(cornerRadius: 8)
//                    .fill(colors[WebWrapperMgr.shared.store.firstIndex(of: webWrapper) ?? 0])
                    .fill(Color(.darkGray))
                    .frame(width:screen_width - 48 ,height: 40)
                    .overlay {
                        GeometryReader { geometry in
                            VStack(alignment: .leading, spacing: 0) {
                                ProgressView(value: webWrapper.estimatedProgress)
                                    .progressViewStyle(LinearProgressViewStyle())
                                    .foregroundColor(.blue)
                                    .background(Color(white: 1))
                                    .cornerRadius(4)
                                    .frame(height: webWrapper.estimatedProgress >= 1.0 ? 0 : 3)
                                    .alignmentGuide(.leading) { d in
                                        d[.leading]
                                    }
                                    .opacity(webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 ? 1 : 0)
                                
                            }
                            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                
                
                TextField("", text: $inputText)
                    .placeholder(when: inputText.isEmpty) {
                        Text("请输入搜索内容").foregroundColor(Color(white: 0.8))
//                        Text(webWrapper.title ?? "").foregroundColor(Color(white: 0.8))
                    }
                    .background(Color(.darkGray))
                    .foregroundColor(.white)
                    .padding(.horizontal,30)
                    .zIndex(1)
                    .keyboardType(.webSearch)
                    .focused($isAdressBarFocused)
                    .onAppear{
                        print("aaaaaa")
                    }
                    .onTapGesture {
                        print("tapped")
                        isAdressBarFocused = true
                    }
            }
            .frame(height: addressBarH)
        }
    }
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
//        AddressBar(webWrapper: WebWrapper(webCache: WebCache()))
//            .environmentObject(BrowerVM())
    }
}
