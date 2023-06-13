//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import SwiftUI


struct AddressBarHStack: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var addrBarOffset: AddrBarOffset
    @State var tappedInTFArea: Bool = false

    var body: some View {
        PagingScroll(contentSize: WebWrapperMgr.shared.store.count, offsetX: $addrBarOffset.onX, indexInEvrm: $selectedTab.curIndex, content: {
            HStack(spacing: 0) {
                ForEach(WebWrapperMgr.shared.store, id: \.id){ webWrapper in
                    ZStack {
                        AddressBar(webWrapper: webWrapper, tappedInTFArea: $tappedInTFArea)
                            .frame(width: screen_width)
                    }
                }
            }
            .background(.white)
        })
//        .frame(width: screen_width)
        .frame(height: toolbarState.addressBarHeight)
        .animation(.easeInOut, value: toolbarState.addressBarHeight)
        .transition(.slide)
        .onTapGesture { tapPoint in
            print(tapPoint)
            tappedInTFArea = tapInsideTextfield(at: tapPoint)
        }
    }
    func tapInsideTextfield(at tapPoint: CGPoint) -> Bool{
        let tfLeadingTopPoint = CGPoint(x: 32, y: 14)
        let tfTrealingBottomPoint = CGPoint(x: screen_width - 32*2, y: 14 + 28)

        return tapPoint.x >= tfLeadingTopPoint.x
                && tapPoint.x <= tfTrealingBottomPoint.x
                && tapPoint.y >= tfLeadingTopPoint.y
                && tapPoint.y <= tfTrealingBottomPoint.y
    }
}

struct AddressBar: View {
    @FocusState var isAdressBarFocused: Bool
    @ObservedObject var webWrapper: WebWrapper
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @Binding var tappedInTFArea: Bool
    
    var isVisible: Bool { return WebWrapperMgr.shared.store.firstIndex(of: webWrapper) ==  selectedTab.curIndex }
    
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 }
    
    var body: some View {
        
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
            // 使用自定义的UITextField
            TextField("", text: $addressBar.inputText)
                .placeholder(when: addressBar.inputText.isEmpty) {
                    Text(webWrapper.title!).foregroundColor(Color(white: 0.8))
                }
                .frame(height: 28)
                .background(Color(.darkGray))
                .foregroundColor(.white)
                .padding(.horizontal,30)
                .keyboardType(.webSearch)
                .focused($isAdressBarFocused)
                .onAppear{
                    print()
                }
                .onChange(of: tappedInTFArea) { focused in
                    if focused, isVisible{
                        isAdressBarFocused = focused
                        addressBar.isFocused = true
                        tappedInTFArea = false
                    }
                }
                .onChange(of: addressBar.isFocused) { isFocused in
                    if !isFocused{
                        isAdressBarFocused = isFocused
                        // cancel search, should reset url to textfield
                    }
                }
        }
        .frame( height: addressBarH)
    }
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
       
    }
}
