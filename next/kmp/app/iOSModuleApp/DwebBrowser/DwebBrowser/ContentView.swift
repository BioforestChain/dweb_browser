//
//  ContentView.swift
//  DwebBrowser
//
//  Created by instinct on 2023/12/27.
//

import SwiftUI
import DwebWebBrowser

struct ContentView: View {
    var body: some View {
        ZStack {
            BrowserView()
                .ignoresSafeArea()
        }
        .padding()
    }
}

#Preview {
    ContentView()
        .ignoresSafeArea()
}
