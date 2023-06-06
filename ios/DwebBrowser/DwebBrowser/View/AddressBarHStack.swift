//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import SwiftUI

struct AddressBarHStack: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var tabState: TabState
    @EnvironmentObject var addrBarOffset: AddrBarOffset

    var body: some View {
        PagingScroll(contentSize: WebWrapperMgr.shared.store.count, content: AddressBarHContainer(), offsetX: $addrBarOffset.onX, indexInEvrm: $selectedTab.curIndex)
        
            .frame(height: tabState.addressBarHeight)
            .animation(.easeInOut, value: tabState.addressBarHeight)
            .transition(.slide)
    }
}

struct AddressBarHContainer:View{
    @StateObject var addressBar = AddressBarState()
    var body: some View{
        HStack(spacing: 0) {
            ForEach(WebWrapperMgr.shared.store){ webWrapper in
                AddressBar(webWrapper: webWrapper, addressBar: addressBar)
                    .frame(width: screen_width)
            }
        }
        .background(.white)
    }
}

struct AddressBar: View {
    @FocusState var isAdressBarFocused: Bool
    @ObservedObject var webWrapper: WebWrapper
    @ObservedObject var addressBar: AddressBarState
    
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 }
    
    var body: some View {
        GeometryReader{ geometry in
            
            ZStack() {
                Color(.white)
                RoundedRectangle(cornerRadius: 8)
//                    .fill(colors[WebWrapperMgr.shared.store.firstIndex(of: webWrapper) ?? 0])
                    .fill(colors[WebWrapperMgr.shared.store.firstIndex(of: webWrapper) ?? 0])
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
                                    .opacity(shouldShowProgress ? 1 : 0)
                            }
                            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                
                TextField("", text: $addressBar.inputText)
                    .placeholder(when: addressBar.inputText.isEmpty) {
                        Text(webWrapper.title!).foregroundColor(Color(white: 0.8))
                    }
                    .background(Color(.darkGray))
                    .foregroundColor(.white)
                    .padding(.horizontal,30)
                    .keyboardType(.webSearch)
                    .focused($isAdressBarFocused)

                    .onChange(of: isAdressBarFocused) { focused in
                        addressBar.isFocused = focused
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
