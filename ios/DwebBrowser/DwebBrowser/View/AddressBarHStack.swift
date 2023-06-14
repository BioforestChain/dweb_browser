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
    @State var tappedPoint: CGPoint = .zero

    var body: some View {
        PagingScroll(contentSize: WebWrapperMgr.shared.store.count, offsetX: $addrBarOffset.onX, selectedIndex: $selectedTab.curIndex, content: {
            HStack(spacing: 0) {
                ForEach(WebWrapperMgr.shared.store, id: \.id){ webWrapper in
                    AddressBar3(webWrapper: webWrapper, tappedPoint: $tappedPoint)
                        .frame(width: screen_width)
                        .offset(y:5)
                        .background(Color.bkColor)
                }
            }
        })
        .frame(height: toolbarState.addressBarHeight)
        .animation(.easeInOut, value: toolbarState.addressBarHeight)
        .transition(.slide)
        .onTapGesture { tapPoint in
            print(tapPoint)
                tappedPoint = tapPoint
        }
    }
}

struct AddressBar3: View {
    @FocusState var isAdressBarFocused: Bool
    @ObservedObject var webWrapper: WebWrapper
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    
    @Binding var tappedPoint: CGPoint
    @State private var textFieldFrame: CGRect = .zero
    @State private var buttonFrame: CGRect = .zero
    
    @State var tappedInTFArea: Bool = false
    
    var isVisible: Bool { return WebWrapperMgr.shared.store.firstIndex(of: webWrapper) ==  selectedTab.curIndex }
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 }
    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 10)
                .foregroundColor(.white)
                .frame(height: 40)
                .overlay {
                    progressV
                }
                .padding(.horizontal)
                .background(GeometryReader { geometry in
                                    Color.clear
                                        .onAppear {
                                            textFieldFrame = geometry.frame(in: .local)
                                            print("textFieldFrame: \(textFieldFrame)")
                                        }
                                })
            HStack {
                TextField("", text: $addressBar.inputText)
                    .placeholder(when: addressBar.inputText.isEmpty) {
                        Text(searchTextFieldPlaceholder).foregroundColor(Color.lightTextColor)
                    }
                    .foregroundColor(.black)
                    .padding(.horizontal, 25)
                    .keyboardType(.webSearch)
                    .focused($isAdressBarFocused)
                    .onChange(of: tappedInTFArea) { focused in
                        if isVisible, focused{
                            isAdressBarFocused = focused
                            addressBar.isFocused = true
                            tappedInTFArea = false
                        }
                    }
                    .onChange(of: addressBar.isFocused) { isFocused in
                        if !isFocused{
                            isAdressBarFocused = isFocused
                        }
                    }
                
                if !addressBar.inputText.isEmpty{
                    Spacer()
                    Button {
                        addressBar.inputText = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                            .padding(.trailing, 18)
                    }
                    .background(GeometryReader { geometry in
                                    Color.clear
                                        .onAppear {
                                            buttonFrame = geometry.frame(in: .local)
                                        }
                                })
                }
            }
        }
        .padding()
        .onChange(of: tappedPoint) { point in
            if isVisible {
                if textFieldFrame.contains(point){
                    tappedInTFArea = true
                }else if buttonFrame.contains(point){
                    addressBar.inputText = ""
                }
            }
        }
    }
    
    var progressV: some View{
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
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
        
    }
}
