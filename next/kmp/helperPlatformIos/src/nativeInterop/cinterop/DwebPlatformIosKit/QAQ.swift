//
//  QAQ.swift
//  DwebPlatformIosKit
//
//  Created by kzf on 2023/11/16.
//

import Foundation

@objc
open class IosKit: NSObject {
  override init() {
    let a = 1
    let b = 2
    print("a+b=\(a)+\(b)=\(a + b)")
  }

  @objc
  open func add(a: Int, b: Int) -> Int {
    print("a+b=\(a)+\(b)=\(a + b)")
    return a + b
  }
}

@objc
open class IosKit2: NSObject {
  @objc
  open func add(a: Int, b: Int) -> Int {
    print("a+b=\(a)+\(b)=\(a + b)")
    return a + b
  }
}

@objc
open class IosKit3: NSObject {
  @objc
  open func add(a: Int, b: Int, c: Int, d: Int, e: Int) -> Int {
    print("a+b=\(a)+\(b)=\(a + b)")
    return a + b + c + d + e
  }
}

