//
//  CombinePublisher.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import Combine

let homeDataPublisher = PassthroughSubject<[[String:String]], Never>()
let clickHomeAppPublisher = PassthroughSubject<String, Never>()
