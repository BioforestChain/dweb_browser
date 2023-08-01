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
    @ObservedObject var webWrapper: WebWrapper
    @ObservedObject var webCache: WebCache

    @FocusState var isAdressBarFocused: Bool
    @State private var inputText: String = ""

    private var isVisible: Bool { return WebWrapperMgr.shared.store.firstIndex(of: webWrapper) == selectedTab.curIndex }
    private var shouldShowProgress: Bool { webWrapper.estimatedProgress > 0.0 && webWrapper.estimatedProgress < 1.0 && !addressBar.isFocused }
    private var domainString: String { webCache.isBlank() ? addressbarHolder : webCache.lastVisitedUrl.getDomain() }
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
                        reloadButton
                    }
                }
            }

            textField
                .foregroundColor(.black)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .padding(.leading, 24)
                .padding(.trailing, 50)
                .keyboardType(.webSearch)
                .focused($isAdressBarFocused)
                .opacity(isOpacity())

            #if DwebBrowser
                Text(domainString)
                    .frame(width: screen_width - 100, height: 32)
                    .background(.white)
                    .foregroundColor(webCache.isBlank() ? .networkTipColor : .black)
                    .padding(.horizontal, 50)
                    .opacity(isAdressBarFocused ? 0 : 1)
                    .onTapGesture {
                        if webCache.isBlank() {
                            inputText = ""
                        }
                        isAdressBarFocused = true
                    }
            #endif
        }
        .background(Color.bkColor)
    }

    var textField: some View {
        TextField(addressbarHolder, text: $inputText)
        #if DwebFramework
            .onTapGesture {
                if webCache.isBlank() {
                    inputText = ""
                }
                isAdressBarFocused = true
                addressBar.isFocused = true
            }
        #endif
            .onAppear {
                inputText = webCache.lastVisitedUrl.absoluteString
            }
            .onChange(of: webCache.lastVisitedUrl) { url in
                inputText = url.absoluteString
            }
            .onChange(of: isAdressBarFocused, perform: { isFocued in
                #if DwebBrowser
                    addressBar.isFocused = isFocued
                #endif

                #if DwebFramework
                    ConsoleSwift.inject("isAdressBarFocused \(isAdressBarFocused)--addressBar.isFocused: \(addressBar.isFocused) --isFocued \(isFocued)")
                #endif
            })
            .onChange(of: addressBar.isFocused) { isFocused in
                print("addressBar.isFocused \(addressBar.isFocused) -- \(isFocused)")
                if !isFocused, isVisible {
                    #if DwebBrowser
                        isAdressBarFocused = isFocused
                    #endif
                    if addressBar.inputText.isEmpty { // 点击取消按钮
                        inputText = webCache.lastVisitedUrl.absoluteString
                    }
                }
                #if DwebFramework
                    ConsoleSwift.inject("2222addressBar.isFocused \(addressBar.isFocused) -- \(isFocused) -- \(isAdressBarFocused)")
                #endif
            }
            .onSubmit {
                let url = URL.createUrl(inputText)
                let webcache = WebCacheMgr.shared.store[index]
                if !webcache.shouldShowWeb {
                    webcache.shouldShowWeb = true
                }
                openingLink.clickedLink = url
                isAdressBarFocused = false
                #if DwebFramework
                    addressBar.isFocused = false
                #endif
            }
            .onReceive(NotificationCenter.default.publisher(for: UITextField.textDidBeginEditingNotification)) { obj in

                if let textField = obj.object as? UITextField {
                    textField.selectedTextRange = textField.textRange(from: textField.beginningOfDocument, to: textField.endOfDocument)
                }
            }
            .onChange(of: inputText) { text in
                #if DwebFramework
                    ConsoleSwift.inject(text)
                #endif
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
            #if DwebFramework
                isAdressBarFocused = true
                addressBar.isFocused = true
            #endif

            #if DwebFramework
                ConsoleSwift.inject("clearTextButton \(addressBar.isFocused) -- \(isAdressBarFocused)")
            #endif
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

    func isOpacity() -> CGFloat {
        #if DwebFramework
            return 1
        #endif

        return isAdressBarFocused ? 1 : 0
    }
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("")
    }
}
