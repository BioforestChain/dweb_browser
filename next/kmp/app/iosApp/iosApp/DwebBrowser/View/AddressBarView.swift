//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import Combine
import SwiftUI

struct AddressBar: View {
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var dragScale: WndDragScale
    @EnvironmentObject var cacheStore: WebCacheStore
    
    @ObservedObject var webWrapper: WebWrapper
    @ObservedObject var webCache: WebCache

    @FocusState var isAdressBarFocused: Bool
    @State private var inputText: String = ""
    @State private var displayText: String = ""
    @State private var loadingProgress: CGFloat = 0
    
    private var tabIndex: Int { cacheStore.index(of: webCache)! }
    private var isVisible: Bool { tabIndex == selectedTab.curIndex }
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 && !addressBar.isFocused }
    private var textColor: Color { isAdressBarFocused ? .black : webCache.isBlank() ? .networkTipColor : .black }

    var roundRectHeight: CGFloat {dragScale.addressbarHeight / 1.4}

    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 10)
                .foregroundColor(Color.AddressbarbkColor)
                .overlay {
                    progressV
                }
                .padding(.horizontal)
                .frame(height: roundRectHeight)
            
            textField
            accessoryButtons
        }
        .frame(height: dragScale.addressbarHeight)
    }
    
    var accessoryButtons: some View {
            HStack{
                Spacer()
                ZStack{
                    if addressBar.isFocused, !inputText.isEmpty {
                        clearTextButton
                    }
                    
                    if !inputText.isEmpty, !addressBar.isFocused {
                        if shouldShowProgress {
                            cancelLoadingButtion
                        } else {
                            if !webCache.isBlank() {
                                reloadButton
                            }
                        }
                    }
                }
                .frame(width: roundRectHeight)
                .scaleEffect(dragScale.onWidth)
                Spacer().frame(width: 25)
            }
    }

    var textField: some View {
        TextField(addressbarHolder, text: addressBar.isFocused ? $inputText : $displayText)
            .foregroundColor(Color.addressTextColor)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
            .multilineTextAlignment(addressBar.isFocused ? .leading : .center)
            .padding(.leading, 24)
            .padding(.trailing, 50)
            .keyboardType(.webSearch)
            .focused($isAdressBarFocused)
            .font(dragScale.scaledFont())
            .onTapGesture {
                if webCache.isBlank() {
                    inputText = ""
                }
                isAdressBarFocused = true
                addressBar.isFocused = true
            }

            .onAppear {
                inputText = webCache.lastVisitedUrl.absoluteString
                displayText = webCache.isBlank() ? addressbarHolder : webCache.lastVisitedUrl.getDomain()
            }
            .onChange(of: webCache.lastVisitedUrl, initial:false, { _, newValue in
                inputText = newValue.absoluteString
                displayText = webCache.isBlank() ? addressbarHolder : webCache.lastVisitedUrl.getDomain()
            })
            .onChange(of: webCache.lastVisitedUrl) { _, url in
                inputText = url.absoluteString
                displayText = webCache.isBlank() ? addressbarHolder : webCache.lastVisitedUrl.getDomain()
            }
            .onChange(of: addressBar.isFocused) { _, isFocused in
                Log( " addressBar.isFocused onChange:\(isFocused)")
                if !isFocused, isVisible {
                    isAdressBarFocused = isFocused
                    if addressBar.inputText.isEmpty { // 点击取消按钮
                        inputText = webCache.lastVisitedUrl.absoluteString
                    }
                }
            }
            .onSubmit {
                let url = URL.createUrl(inputText)
                DispatchQueue.main.async {
                    if !webCache.shouldShowWeb {
                        webCache.lastVisitedUrl = url
                    }
                    openingLink.clickedLink = url
                    isAdressBarFocused = false
                    addressBar.isFocused = false
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UITextField.textDidBeginEditingNotification)) { obj in
                if let textField = obj.object as? UITextField {
                    textField.selectedTextRange = textField.textRange(from: textField.beginningOfDocument, to: textField.endOfDocument)
                }
            }
            .onChange(of: inputText) { _, text in
                addressBar.inputText = text
            }
            .onChange(of: openingLink.clickedLink){ _, _ in
                loadingProgress = 0
            }
    }

    var progressV: some View {
        GeometryReader { geometry in
            VStack(alignment: .leading, spacing: 0) {
                ProgressView(value: loadingProgress)
                    .progressViewStyle(LinearProgressViewStyle())
                    .foregroundColor(.blue)
                    .background(Color(white: 1))
                    .cornerRadius(4)
                    .frame(height: loadingProgress >= 1.0 ? 0 : 3)
                    .alignmentGuide(.leading) { d in
                        d[.leading]
                    }
                    .opacity(shouldShowProgress ? 1 : 0)
            }
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .onChange(of: webWrapper.estimatedProgress){ _, progress in
                loadingProgress = progress
            }
        }
    }

    var clearTextButton: some View {
        Button {
            inputText = ""
        } label: {
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(Color.clearTextColor)
        }
    }

    var reloadButton: some View {
        Button {
            addressBar.needRefreshOfIndex = tabIndex
        } label: {
            Image(systemName: "arrow.clockwise")
                .foregroundColor(Color.addressTextColor)
        }
    }

    var cancelLoadingButtion: some View {
        Button {
            addressBar.stopLoadingOfIndex = tabIndex
        } label: {
            Image(systemName: "xmark")
                .foregroundColor(Color.addressTextColor)
        }
    }
}
