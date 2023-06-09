//
//  CombinePublisher.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/6.
//

import Combine
import WebKit

let progressPublisher = PassthroughSubject<Float, Never>()
let addWebViewPublisher = PassthroughSubject<WKWebView, Never>()
let homeDataPublisher = PassthroughSubject<[[String:String]], Never>()
let clickHomeAppPublisher = PassthroughSubject<String, Never>()
let clickAddButtonPublisher = PassthroughSubject<String, Never>()
