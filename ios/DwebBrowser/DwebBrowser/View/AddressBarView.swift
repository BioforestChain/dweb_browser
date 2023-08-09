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

    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var keyboard: KeyBoard
    @ObservedObject var webWrapper: WebWrapper
    @ObservedObject var webCache: WebCache

    @FocusState var isAdressBarFocused: Bool
    @State private var inputText: String = ""
    @State private var displayText: String = ""

    private var isVisible: Bool { index == selectedTab.curIndex }
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 && !addressBar.isFocused }
    private var textColor: Color { isAdressBarFocused ? .black : webCache.isBlank() ? .networkTipColor : .black }

    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 10)
                .foregroundColor(.white)
                .frame(height: 40)
                .overlay {
                    progressV
                }
                .padding(.horizontal)

            HStack {
                if addressBar.isFocused, !inputText.isEmpty {
                    Spacer()
                    clearTextButton
                }

                if !inputText.isEmpty, !addressBar.isFocused {
                    Spacer()
                    if shouldShowProgress {
                        cancelLoadingButtion
                    } else {
                        if !webCache.isBlank() {
                            reloadButton
                        }
                    }
                }
            }

            textField
        }
        .background(Color.bkColor)
    }

    var textField: some View {
        TextField(addressbarHolder, text: addressBar.isFocused ? $inputText : $displayText)
            .foregroundColor(textColor)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
            .multilineTextAlignment(addressBar.isFocused ? .leading : .center)
            .padding(.leading, 24)
            .padding(.trailing, 50)
            .keyboardType(.webSearch)
            .focused($isAdressBarFocused)
            .onTapGesture {
                if webCache.isBlank() {
                    inputText = ""
                }
                isAdressBarFocused = true
                addressBar.isFocused = true
            }

            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { notify in
                if isVisible {
                    guard let value = notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
                    keyboard.height = value.height
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
                if isVisible {
                    keyboard.height = 0
                }
            }
            .onAppear {
                inputText = webCache.lastVisitedUrl.absoluteString
                displayText = webCache.isBlank() ? addressbarHolder : webCache.lastVisitedUrl.getDomain()
            }
            .onChange(of: webCache.lastVisitedUrl) { url in
                inputText = url.absoluteString
                displayText = webCache.isBlank() ? addressbarHolder : webCache.lastVisitedUrl.getDomain()
            }
            .onChange(of: addressBar.isFocused) { isFocused in
                printWithDate(msg: " addressBar.isFocused onChange:\(isFocused)")
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
                    let webcache = WebCacheMgr.shared.store[index]
                    if !webcache.shouldShowWeb {
                        webcache.lastVisitedUrl = url
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
            .onChange(of: inputText) { text in
                addressBar.inputText = text
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
        }
    }

    var clearTextButton: some View {
        Button {
            inputText = ""
        } label: {
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(.gray)
                .padding(.trailing, 20)
        }
    }

    var reloadButton: some View {
        Button {
            addressBar.needRefreshOfIndex = index
        } label: {
            Image(systemName: "arrow.clockwise")
                .foregroundColor(.black.opacity(0.9))
                .padding(.trailing, 25)
        }
    }

    var cancelLoadingButtion: some View {
        Button {
            addressBar.stopLoadingOfIndex = index
        } label: {
            Image(systemName: "xmark")
                .foregroundColor(.black.opacity(0.8))
                .padding(.trailing, 25)
        }
    }
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
    }
}
