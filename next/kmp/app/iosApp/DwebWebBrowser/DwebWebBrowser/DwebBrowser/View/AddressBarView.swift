//
//  AddressBarHub.swift
//  TableviewDemo
//
//  Created by ui06 on 4/14/23.
//

import Combine
import SwiftUI

struct AddressBar: View {
    @Environment(AddressBarState.self) var addressBar
    @Environment(WndDragScale.self) var dragScale
    @Environment(OpeningLink.self) var openingLink
    @Environment(WebMonitor.self) var webMonitor
    @Environment(OuterSearch.self) var outerSearch

    var webCache: WebCache
    let tabIndex: Int
    let isVisible: Bool

    @FocusState var isAdressBarFocused: Bool
    @State private var inputText: String = ""

    @State var showProgress: Double = 0

    private var shouldShowProgress: Bool { webMonitor.loadingProgress > 0.0 && webMonitor.loadingProgress < 1.0 && !addressBar.isFocused }

    var roundRectHeight: CGFloat { dragScale.addressbarHeight / 1.4 }

    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 10)
                .foregroundColor(.addressbarTFbk)
                .overlay {
                    progressV
                }
                .padding(.horizontal)
                .frame(height: roundRectHeight)

            textField
            accessoryButtons
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("AddressBar")
        .onChange(of: webMonitor.loadingProgress) { _, newValue in
            showProgress = newValue
        }
        .onChange(of: outerSearch.content) { _, searchedText in
            if searchedText != "" {
                inputText = searchedText
                isAdressBarFocused = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    outerSearch.shouldDoSearch = false
                }
            }
        }
        .frame(height: dragScale.addressbarHeight)
    }

    var accessoryButtons: some View {
        HStack {
            Spacer()
            ZStack {
                if addressBar.isFocused, !inputText.isEmpty {
                    clearTextButton
                }

                if !inputText.isEmpty, !addressBar.isFocused {
                    if shouldShowProgress {
                        cancelLoadingButton
                    } else {
                        if webCache.isWebVisible {
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
        TextField(addressbarHolder, text: $inputText)
            .foregroundColor(.primary)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
            .multilineTextAlignment(isAdressBarFocused ? .leading : .center)
            .padding(.leading, 24)
            .padding(.trailing, 60)
            .keyboardType(.webSearch)
            .focused($isAdressBarFocused)
            .font(dragScale.scaledFont_18)
            .accessibilityIdentifier("Address")
            .onChange(of: isAdressBarFocused) { _, focused in
                if focused {
                    addressBar.isFocused = true
                }
            }

            .onAppear {
                inputText = webCache.lastVisitedUrl.domain
                if inputText == emptyLink {
                    inputText = ""
                }
            }

            .onChange(of: webCache.lastVisitedUrl) { _, url in
                inputText = url.domain
            }
            .onChange(of: addressBar.isFocused) { _, isFocused in
                if isVisible {
                    if !outerSearch.shouldDoSearch{
                        inputText = isFocused ? webCache.lastVisitedUrl.absoluteString : webCache.lastVisitedUrl.domain
                        if inputText == emptyLink {
                            inputText = ""
                        }
                    }
                }
                if isFocused == false {
                    isAdressBarFocused = false
                }
            }
//            .onChange(of: openingLink.outerSearchText) { _, searchedText in
//                if searchedText != "" {
//                    inputText = searchedText
//                    isAdressBarFocused = true
//                }
//            }
            .onSubmit {
                let url = URL.createUrl(inputText)
                openingLink.clickedLink = url
                addressBar.isFocused = false
                showProgress = 0
            }
            .onChange(of: inputText) { _, text in
                addressBar.inputText = text
            }
    }

    var progressV: some View {
        GeometryReader { geometry in
            VStack(alignment: .leading, spacing: 0) {
                ProgressView(value: showProgress)
                    .progressViewStyle(LinearProgressViewStyle())
                    .foregroundColor(.blue)
                    .background(Color(white: 1))
                    .cornerRadius(4)
                    .frame(height: showProgress >= 1.0 ? 0 : 3)
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
                .foregroundColor(Color.primary)
        }
    }

    var reloadButton: some View {
        Button {
            addressBar.needRefreshOfIndex = tabIndex
        } label: {
            Image(systemName: "arrow.clockwise")
                .foregroundColor(Color.primary)
        }
    }

    var cancelLoadingButton: some View {
        Button {
            addressBar.stopLoadingOfIndex = tabIndex
        } label: {
            Image(systemName: "xmark")
                .foregroundColor(Color.primary)
        }
    }
}
