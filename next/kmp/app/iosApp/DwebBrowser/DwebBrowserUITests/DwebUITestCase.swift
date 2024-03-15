//
//  DwebUITestCase.swift
//  DwebBrowserUITests
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import XCTest

let webBrowserLaunchType = true

class DwebUITestCase: XCTestCase {
    
    let app = XCUIApplication()
    
    override func setUpWithError() throws {
        //app.launchEnvironment = ["DWEB_TEST_MODE" : "DwebUITesting"]
        app.launch()
//        if webBrowserLaunchType{
//            Task {
//                await launchWebBrowserByTap()
//            }
//        } else {
//            launchWebBrowserByDeepLink()
//        }
    }
    

    //MARK: - Dweb browser components
    var tabsContainer: XCUIElement {
        let element = app.descendants(matching: .other).matching(identifier: "TabsContainer").firstMatch
        XCTAssert(element.exists)
        return element
    }
    
    var toolBarView: XCUIElement {
        let element = app.descendants(matching: .other).matching(identifier: "ToolbarView").firstMatch
        XCTAssert(element.exists)
        return element
    }
    
    var addressBar: XCUIElement {
        let element = tabsContainer.descendants(matching: .other).matching(identifier: "AddressBar").firstMatch
        XCTAssert(element.exists)
        return element
    }
        
    var addressTF: XCUIElement {
        let element = app.textFields["Address"]
        XCTAssert(element.exists)
        return element
    }
    
    var addressClear: XCUIElement? {
        let element = addressBar.buttons["Close"].firstMatch
        // 地址栏的清除按钮比较特殊，只有在addressTF有输入内容的时候才会出现。
        if element.exists {
            return element
        } else {
            return nil
        }
    }
    
    var searchView: XCUIElement {
        print("dddddd:\(app.debugDescription)")
        let element = app.otherElements["SearchResultView"]
        XCTAssert(element.exists)
        return element
    }
    
    var baiduSearcher: XCUIElement {
        let element = searchView.staticTexts["baidu"]
        XCTAssert(element.exists)
        return element
    }
    
    var sogouSearcher: XCUIElement {
        let element = searchView.staticTexts["sogou"]
        XCTAssert(element.exists)
        return element
    }
    
    var so360Searcher: XCUIElement {
        let element = searchView.staticTexts["360so"]
        XCTAssert(element.exists)
        return element
    }
    
    var shiftButton: XCUIElement {
        let element = app.buttons["shift"]
        XCTAssert(element.exists)
        return element
    }
    
    var addButton: XCUIElement {
        let element = app.buttons["add"]
        XCTAssert(element.exists)
        return element
    }
    
    var scanButton: XCUIElement {
        let element = app.buttons["scan"]
        XCTAssert(element.exists)
        return element
    }
    
    var shortcutButton: XCUIElement {
        let element = app.buttons["shortcut"]
        XCTAssert(element.exists)
        return element
    }
    
    var moreButton: XCUIElement {
        let element = app.buttons["more"]
        XCTAssert(element.exists)
        return element
    }
    
    var morePicker: XCUIElement {
        let element = app.otherElements["SheetSegmentView"]
        XCTAssert(element.exists)
        return element
    }
    
    var moreSegmentedControl: XCUIElement {
        // 🤷🏻‍♀️🥶
        // let element = app.otherElements["morePicker"].firstMatch
        let element = morePicker.descendants(matching: .segmentedControl).firstMatch
        XCTAssert(element.exists)
        return element
    }
    
    var bookmarksView: XCUIElement {
        let element = app.otherElements["BookmarkView"]
        return element
    }
    
    var bookmarksList: XCUIElement {
        let element = bookmarksView.collectionViews["BookmarkView_List"]
        return element
    }
    
    var bookmarksEmpty: XCUIElement {
        let element = bookmarksView.otherElements["BookmarkView_Empty"]
        return element
    }
    
    var menuView: XCUIElement {
        print("MenuView: \(app.debugDescription)")
        let element = morePicker.scrollViews["MenuView"]
        return element
    }
        
    var historysView: XCUIElement {
        print("HistoryView: \(morePicker.debugDescription)")
        let element = morePicker.otherElements["HistoryView"]
        return element
    }
    
    var historysViewList: XCUIElement {
        let element = historysView.collectionViews["HistoryView_List"]
        return element
    }
    
    var historysViewEmpty: XCUIElement {
        let element = historysView.otherElements["HistoryView_Empty"]
        return element
    }

    
    //MARK: - tap
    @MainActor
    @inline(__always) func tapWebBrowserIcon() {
        // 比较脆弱的方式，不过暂时够用。
        let browser = app.staticTexts["Browser"]
        XCTAssert(browser.waitForExistence(timeout: 0.3))
        browser.tap()
//        let coord = app.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.25))
//        coord.tap()
    }
    
    @MainActor
    func tapMoreSegmentedControl(_ index: Int = 0) {
        moreSegmentedControl.buttons.element(boundBy: index).forceTapElement()
    }
    
    //MARK: - gesture
    @MainActor
    func doLeftScreenPanGesture() {
        let coord0 = app.coordinate(withNormalizedOffset: CGVector(dx: -0.1, dy: 0.5))
        let coord1 = coord0.withOffset(CGVector(dx: 180, dy: 0))
        coord0.press(forDuration: 0.5, thenDragTo: coord1)
    }
    
    @MainActor
    func doRightScreenPanGesture() {
        let coord0 = app.coordinate(withNormalizedOffset: CGVector(dx: 1.1, dy: 0.5))
        let coord1 = coord0.withOffset(CGVector(dx: -180, dy: 0))
        coord0.press(forDuration: 0.5, thenDragTo: coord1)
    }
    
    @MainActor
    func doCloseMorePicker() {
        let moreHandle = app.otherElements["moreHandle"]
        XCTAssert(moreHandle.exists)
        let coord0 = moreHandle.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        let coord1 = coord0.withOffset(CGVector(dx: 0.0, dy: 100))
        coord0.press(forDuration: 1, thenDragTo: coord1)
    }
    
    //MARK: - typeText
    @MainActor
    func doLoadWebSite(url: String) async {
        addressTF.tap()
        await delay(0.3) { addressClear?.tap() }
        addressTF.typeText(url)
        XCTAssert(app.buttons["go"].exists)
        app.buttons["go"].tap()
        await delay(2) //等待网页加载完毕
    }
    
    @MainActor
    func doBaiduSearch(_ keyword: String) async {
        addressTF.tap()
        await delay(0.3) { addressClear?.tap() }
        addressTF.typeText(keyword)
        baiduSearcher.tap()
        await delay(2) //等待网页加载完毕
    }
    
    @MainActor
    func doSogouSearch(_ keyword: String) async {
        addressTF.tap()
        await delay(0.3) { addressClear?.tap() }
        addressTF.typeText(keyword)
        sogouSearcher.tap()
        await delay(2) //等待网页加载完毕
    }
    
    @MainActor
    func doSo360Search(_ keyword: String) async {
        addressTF.tap()
        await delay(0.3) { addressClear?.tap() }
        addressTF.typeText(keyword)
        so360Searcher.tap()
        await delay(2) //等待网页加载完毕
    }
    
    //MARK: - Check
    @MainActor
    func checkWebBrowser(_ exists: Bool = true) async {
        XCTAssert(app.otherElements["Web browser"].waitForExistence(timeout: 0.5) == exists)
    }
    
    //MARK: - Clear
    @MainActor
    func closeAllWebSites() async {
        shiftButton.forceTapElement()
        await delay(0.3)
        let qe = tabsContainer.descendants(matching: .button).matching(identifier: "Close")
        await qe.allElementsBoundByAccessibilityElement.asyncForEach { e in
            e.forceTapElement()
            await delay(0.1)
        }
        await delay(0.3)
    }
    
    @MainActor
    func deleteAllBookmarks() async {
        moreButton.forceTapElement()
        tapMoreSegmentedControl(1)
        bookmarksView.swipeUp()
        await morePicker.staticTexts.allElementsBoundByAccessibilityElement.reversed().asyncForEach { e in
            e.swipeLeft()
            XCTAssert(morePicker.buttons["Delete"].exists)
            morePicker.buttons["Delete"].tap()
            await delay(0.3)
        }
        doCloseMorePicker()
    }
    
    //MARK: - launch
    @MainActor
    func launchWebBrowser() async {
        await launchWebBrowserByTap()
        await checkWebBrowser()
    }
    
    @MainActor
    func launchWebBrowserByTap() async {
        tapWebBrowserIcon()
        try? await Task.sleep(nanoseconds: 500_000_000)
    }
    
    @MainActor
    func launchWebBrowserByDeepLink() {
        app.launch()
        XCUIDevice.shared.press(.home)
        
        //
        let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
        safari.launch()
        XCTAssert(safari.wait(for: .runningForeground, timeout: 10))
        
        if safari.buttons["Tabs"].exists {
            
            safari.buttons["Tabs"].tap()
            XCTAssert(safari.buttons["New tab"].waitForExistence(timeout: 0.5))
            safari.buttons["New tab"].tap()
            
            XCTAssert(safari.textFields["Address"].waitForExistence(timeout: 0.5))
            safari.textFields["Address"].tap()
            
            XCTAssert(safari.textFields["URL"].waitForExistence(timeout: 0.5))
            safari.textFields["URL"].typeText("dweb://search?q=Qqqq")
            
            XCTAssert(safari.buttons["go"].waitForExistence(timeout: 0.5))
            safari.buttons["go"].tap()
            
            XCTAssert(safari.buttons["Open"].waitForExistence(timeout: 0.5))
            safari.buttons["Open"].tap()
            
        } else {
            XCTAssert(false)
        }
        
        XCTAssert(app.staticTexts["搜索"].waitForExistence(timeout: 2))
        XCTAssert(app.buttons["取消"].exists)
        app.buttons["取消"].tap()
    }
}

//MARK: - delay
@MainActor
func delay(_ sec: Float) async {
    try? await Task.sleep(nanoseconds: UInt64(sec * 1_000_000_000.0))
}

@MainActor
func delay<T>(_ sec: Float, action: () -> T) async -> T{
    await delay(sec)
    return action()
}

//MARK: - Check
@MainActor
func checkTrue(wait sec: Float, action: @escaping ()->Bool) async {
    await delay(sec) { XCTAssert(action()) }
}

@MainActor
func checkFalse(wait sec: Float, action: @escaping ()->Bool) async {
    await delay(sec) { XCTAssertFalse(action()) }
}

//MARK: - Extension
extension Sequence {
    func asyncForEach(_ action: (Element) async -> Void) async {
        for e in self {
            await action(e)
        }
    }
}

extension XCUIElement {
    
    func forceTapElement() {
        if isHittable {
            tap()
        } else {
            coordinate(withNormalizedOffset: CGVector(dx:0.5, dy:0.5)).tap()
        }
    }
    
    func switcherToggle() {
        switches.firstMatch.tap()
    }
    
    var switcherValue: Bool {
        guard let value = value as? String else {
            XCTAssert(false)
            return false
        }
        return value == "1"
    }
    
    func log(_ type: ElementType, _ flag: String? = nil) {
        let preString = flag != nil ? flag! + ": " : ""
        descendants(matching: type).allElementsBoundByAccessibilityElement.forEach { e in
            print("\(preString)\(e.debugDescription)")
        }
    }
}
