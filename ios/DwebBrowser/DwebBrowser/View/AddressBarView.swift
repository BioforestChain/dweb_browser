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
//    @Binding var visitingUrl: URL

    @FocusState var isAdressBarFocused: Bool
    @State private var inputText: String = ""
    @State private var alignment: TextAlignment = .center
    @State private var addressbarHeight: CGFloat = addressBarH

    private var isVisible: Bool { return WebWrapperMgr.shared.store.firstIndex(of: webWrapper) == selectedTab.curIndex }
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
                    TextField("please input sth", text: $inputText)
                        .foregroundColor(.black)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled(true)
                        .padding(.horizontal, 24)
                        .multilineTextAlignment(alignment)
                        .keyboardType(.webSearch)
                        .focused($isAdressBarFocused)
                        .onAppear {
                            inputText = webCache.lastVisitedUrl.absoluteString
                        }
                        .onChange(of: webCache.lastVisitedUrl) { url  in
                            inputText = url.absoluteString
                        }
                        .onChange(of: isAdressBarFocused, perform: { isFocued in
                            if inputText.isEmpty{
                                alignment = .leading
                            }else{
                                alignment = isFocued ? .leading : .center
                            }
                            addressBar.isFocused = isFocued

                        })
                        .onChange(of: addressBar.isFocused) { isFocused in
                            if !isFocused, isVisible {
                                isAdressBarFocused = isFocused
                                if addressBar.inputText.isEmpty{  //点击取消按钮
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
                            }
                        }
                        .onReceive(NotificationCenter.default.publisher(for: UITextField.textDidBeginEditingNotification)) { obj in
                            if let textField = obj.object as? UITextField {
                                textField.selectedTextRange = textField.textRange(from: textField.beginningOfDocument, to: textField.endOfDocument)
                            }
                        }
//                        .onChange(of: addressBar.inputText) { text in
//                            if text.isEmpty, !isAdressBarFocused{ //  点击取消
//                                inputText = originUrl
//                            }
//                            inputText = text
//                        }
                        .onChange(of: inputText) { text in
                            addressBar.inputText = text
                        }

                    if isAdressBarFocused, !inputText.isEmpty {
                        Spacer()
                        clearTextButton
                    }
                }
            }
            .offset(y: 10)
        }
        .background(Color.bkColor)
    }

//    var addressTextField: some View {
//        TextField(linkString, text: $addressBar.inputText)
//            .placeholder(when: addressBar.inputText.isEmpty) {
//                Text(searchTextFieldPlaceholder).foregroundColor(Color.lightTextColor)
//            }
//            .foregroundColor(.black)
//            .padding(.horizontal, 25)
//            .keyboardType(.webSearch)
//            .focused($isAdressBarFocused)
//
//            .onChange(of: isAdressBarFocused) { focused in
//                if isVisible, focused {
//                    addressBar.isFocused = true
//                }
//            }
//            .onChange(of: addressBar.isFocused) { isFocused in
//                if !isFocused {
//                    isAdressBarFocused = isFocused
//                }
//            }
//            .onSubmit {
//                let url = URL.createUrl(addressBar.inputText)
//                DispatchQueue.main.async {
//                    let webcache = WebCacheMgr.shared.store[index]
//                    if !webcache.shouldShowWeb{
//                        webcache.lastVisitedUrl = url
//                    }
//                    openingLink.clickedLink = url
//                    addressBar.isFocused = false
//                }
//            }
//    }

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
}

struct AddressBarHStack_Previews: PreviewProvider {
    static var previews: some View {
        Text("address bar")
    }
}
