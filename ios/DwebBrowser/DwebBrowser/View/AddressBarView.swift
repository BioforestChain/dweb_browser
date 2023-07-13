//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import Combine
import SwiftUI

struct AddressBar: View {
    var index: Int
    @FocusState var isAdressBarFocused: Bool
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var openingLink: OpeningLink
    
    @ObservedObject var webWrapper: WebWrapper

    var isVisible: Bool { return WebWrapperMgr.shared.store.firstIndex(of: webWrapper) == selectedTab.curIndex }
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 }
    var body: some View {
        GeometryReader { _ in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 10)
                    .foregroundColor(.white)
                    .frame(height: 40)
                    .overlay {
                        progressV
                    }
                    .padding(.horizontal)

                HStack {
                    addressTextField

                    if isVisible, !addressBar.inputText.isEmpty {
                        Spacer()
                        clearTextButton
                    }
                }
            }
            .offset(y: 10)
        }
        .background(Color.bkColor)
        .offset(y: toolbarState.addrBarOffset)
    }

    var addressTextField: some View {
        TextField("", text: $addressBar.inputText)
            .placeholder(when: addressBar.inputText.isEmpty) {
                Text(searchTextFieldPlaceholder).foregroundColor(Color.lightTextColor)
            }
            .foregroundColor(.black)
            .padding(.horizontal, 25)
            .keyboardType(.webSearch)
            .focused($isAdressBarFocused)
            .onChange(of: isAdressBarFocused) { focused in
                if isVisible, focused {
                    addressBar.isFocused = true
                }
            }
            .onChange(of: addressBar.isFocused) { isFocused in
                if !isFocused {
                    isAdressBarFocused = isFocused
                }
            }
            .onSubmit {
                let url = URL.createUrl(addressBar.inputText)
                DispatchQueue.main.async {
                    openingLink.clickedLink = url
                    addressBar.isFocused = false
                }
            }
            .onTapGesture {
                #if DwebFramework
                    addressBar.isFocused = true
                #endif
            }
    }

    var progressV: some View {
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
            .onChange(of: webWrapper.estimatedProgress) { progress in
                printWithDate(msg: "address bar, web index \(index), progress goes \(progress)")
            }
        }
    }

    var clearTextButton: some View {
        Button {
            addressBar.inputText = ""
        } label: {
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(.gray)
                .padding(.trailing, 20)
        }
    }
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("address bar")
    }
}
