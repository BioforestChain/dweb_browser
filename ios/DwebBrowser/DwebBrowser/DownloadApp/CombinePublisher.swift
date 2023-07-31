//
//  CombinePublisher.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/27.
//

import Combine
import WebKit

let progressPublisher = PassthroughSubject<Double, Never>()
let downloadPublisher = PassthroughSubject<Int, Never>()

public typealias onStringCallBack = (String) -> Void
