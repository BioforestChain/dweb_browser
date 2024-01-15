# ``DwebWebBrowser``

DwebWebBrowser

## Overview

DwebWebBrowser是iOS内置浏览器项目。
暴露给KMP使用。
依赖DwebBrowserCommon。

## Topics

- DwebWebView
    * 直接暴露给KMP使用，用于加载iOS浏览器。
    * KMP与DwebWebView是通过protocol进行互操作的。
        + WebBrowserViewDelegate和WebBrowserViewDataSource协议定义规范。WebBrowserViewDataSource提供数据，WebBrowserViewDelegate提供事件或者生命周期。
        + WebBrowserDefaultProvider是默认遵守WebBrowserViewDelegate和WebBrowserViewDataSource的空实现，主要是方便后期UT等预留的。
        + doAction(name: String, params: [String: String]?)和getDatas(for: String, params: [String: AnyObject]?)这是两个KMP与DwebWebView通讯的便捷的方法。只能用于在develop或者debug阶段。
        + iOS集合类型由于kmp在转换时候会丢失具体类型。因而开发者必须自己保证类型的正确，否则，可能会触发swift的类型检查而crash。
    
- Assets & Bundle
    * 由于项目结构的变更，导致DwebWebBrowser不能直接通过Bundle.main的方式获取到资源，因此提供了Bundle+module.swift来解决这个问题。
    * 主要方法：
        + Bundle.browser: 获取DwebWebBrowser下资源文件。
        + Bundle.browserResources:  获取DwebWebBrowser下resource.bundle资源文件。


