//
//  QueueManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/26.
//

import Foundation


public struct Queue<T> {
    
    // 数组用来存储数据元素
    private var data = [T]()
    // 构造方法，用于构建一个空的队列
    public init() {}
    // 构造方法，用于从序列中创建队列
    public init<S: Sequence>(_ elements: S) where S.Iterator.Element == T {
        data.append(contentsOf: elements)
    }
    
    // 将类型为T的数据元素添加到队列的末尾
    public mutating func enqueue(element: T) {
        data.append(element)
    }
    
    // 移除并返回队列中第一个元素, 如果队列不为空，则返回队列中第一个类型为T的元素；否则，返回nil。
    public mutating func dequeue() -> T? {
        return data.removeFirst()
    }
    
    // 返回队列中的第一个元素，但是这个元素不会从队列中删除, 如果队列不为空，则返回队列中第一个类型为T的元素；否则，返回nil
    public func peek() -> T? {
        return data.first
    }
    
    // 清空队列中的数据元素
    public mutating func clear() {
        data.removeAll()
    }
    
    // 返回队列中数据元素的个数
    public var count: Int {
        return data.count
    }
    
    // 返回或者设置队列的存储空间
    public var capacity: Int {
        get {
            return data.capacity
        }
        set {
            data.reserveCapacity(newValue)
        }
    }
    
    // 检查队列是否已满, 如果队列已满，则返回true；否则，返回false
    public func isFull() -> Bool {
        return count == data.capacity
    }
    
    // 检查队列是否为空, 如果队列为空，则返回true；否则，返回false
    public func isEmpty() -> Bool {
        return data.isEmpty
    }
    // 现在Queue的定义中添加一个方法，用于检查索引的合法性
    private func checkIndex(index: Int) {
        if index < 0 || index > count {
            fatalError("Index out of range")
        }
    }
}

// 让打印队列时输出简介的格式
extension Queue: CustomStringConvertible, CustomDebugStringConvertible {
    
    public var description: String {
        return data.description
    }
    
    public var debugDescription: String {
        return data.debugDescription
    }
}

// 让队列支持通过快速声明来创建实例
extension Queue: ExpressibleByArrayLiteral {
    
    public typealias ArrayLiteralElement = T
    
    public init(arrayLiteral elements: T...) {
        self.init(elements)
    }
}

// 扩展队列的for...in循环功能
extension Queue: Sequence {
    
    // 从序列中返回一个迭代器
    public func generate() -> AnyIterator<T> {
        return AnyIterator(IndexingIterator(_elements: data.lazy))
    }
}

// 根据索引返回指定的位置
extension Queue: Collection {
    
    public func index(after i: Int) -> Int {
        return data.index(after: i)
    }
}
// 实现下标功能
extension Queue: MutableCollection {
    // 队列的起始索引
    public var startIndex: Int {
        return 0
    }
    // 队列末尾索引
    public var endIndex: Int {
        return count - 1
    }
    // 获取或者设置下标
    public subscript(index: Int) -> T {
        get {
            checkIndex(index: index)
            return data[index]
        }
        set {
            checkIndex(index: index)
            data[index] = newValue
        }
    }
}
