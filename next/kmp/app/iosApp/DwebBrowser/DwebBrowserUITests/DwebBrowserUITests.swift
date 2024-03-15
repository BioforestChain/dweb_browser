//
//  DwebBrowserUITests.swift
//  DwebBrowserUITests
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import XCTest


final class DwebBrowserUITests: /*XCTestCase*/ DwebUITestCase {

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
    
    /*
        测试正常UI样式
     */
    @MainActor
    func test_webBrowser_UI() async throws {
        await launchWebBrowser()

        let tabsContainer = app.otherElements["TabsContainer"]
        
        // 基本样式检查：
        // 1.TabsContainer
        XCTAssert(tabsContainer.exists)
        XCTAssert(app.otherElements["AddressBar"].exists)
        // 2.ToolbarView
        XCTAssert(app.otherElements["ToolbarView"].exists)
        XCTAssert(app.buttons["shortcut"].exists)
        XCTAssert(app.buttons["shift"].exists)
        XCTAssert(app.buttons["more"].exists)
        
        // 关闭所有网页
        await closeAllWebSites()
        
        // 空页面情况下的样式检查
        await delay(0.5)
        XCTAssert(app.images["dweb_icon"].exists)
        XCTAssert(app.staticTexts["Dweb Browser"].exists)
        XCTAssert(app.buttons["scan"].exists)
        XCTAssertFalse(app.buttons["shortcut"].isEnabled)

        // 加载网页下的样式检查
        await doLoadWebSite(url: "https://www.baidu.com")
        XCTAssert(app.buttons["add"].exists)
        XCTAssertTrue(app.buttons["shortcut"].isEnabled)
    }
    
    /*
     测试屏幕侧滑手势
     */
    @MainActor
    func test_screenEdgePanGesture() async {
        // web browser启动
        await launchWebBrowser()
        
        // 右滑返回
        doLeftScreenPanGesture()
        // web browser消失
        await checkWebBrowser(false)
        
        // 点击web browser
        tapWebBrowserIcon()
        // web browser启动
        await checkWebBrowser(true)

        //左滑返回
        doRightScreenPanGesture()
        // web browser消失
        await checkWebBrowser(false)
    }
    
    /*
     测试网页加载以及网页返回
     */
    @MainActor
    func test_webBrowser_web_load() async throws {
        await launchWebBrowser()
        
        let links: [(String, String)] = [("https://www.baidu.com", "百度一下"),
                                         ("https://www.sogou.com", "搜索")]
        await links.asyncForEach { (url, flag) in
            await doLoadWebSite(url: url)
            XCTAssert(app.buttons[flag].exists)
        }
        
        //
        doLeftScreenPanGesture()
        XCTAssert(app.buttons["百度一下"].waitForExistence(timeout: 1))
    }
    
    /*
     测试toolbar shift + add + done 按钮。
     */
    @MainActor
    func test_webBrowser_toolBar_shift_add_done() async throws {
        await launchWebBrowser()
        
        let tabsContainer = app.descendants(matching: .other).matching(identifier: "TabsContainer").firstMatch
        XCTAssert(tabsContainer.exists)
        
        let toolBarView = app.descendants(matching: .other).matching(identifier: "ToolbarView").firstMatch
        XCTAssert(toolBarView.exists)

        let shiftButton = toolBarView.descendants(matching: .button).matching(identifier: "shift").firstMatch
        
        // case: 检查缩放UI样式
        shiftButton.forceTapElement()
        // tabsContainer:
        XCTAssertFalse(tabsContainer.descendants(matching: .other).matching(identifier: "TabPageView").firstMatch.waitForExistence(timeout: 0.5))
        await checkFalse(wait: 0.5) { tabsContainer.descendants(matching: .other).matching(identifier: "AddressBar").firstMatch.isHittable }
        // toolbar:
        XCTAssert(toolBarView.descendants(matching: .button).matching(identifier: "add").firstMatch.waitForExistence(timeout: 0.5))
        XCTAssert(toolBarView.descendants(matching: .staticText).allElementsBoundByAccessibilityElement.count == 1)
        XCTAssert(toolBarView.descendants(matching: .button).matching(identifier: "done").firstMatch.waitForExistence(timeout: 0.5))
                
        // 清空现有所有的网页缓存。
        var qe = tabsContainer.descendants(matching: .button).matching(identifier: "Close")
        await qe.allElementsBoundByAccessibilityElement.asyncForEach { e in
            e.forceTapElement()
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        
        // case: 检查添加网页功能
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        shiftButton.forceTapElement()
        await (0..<5).asyncForEach { _ in
            let addButton = app.descendants(matching: .button).matching(identifier: "add").firstMatch
            addButton.forceTapElement()
            try? await Task.sleep(nanoseconds: 100_000_000)
            shiftButton.forceTapElement()
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        XCTAssert(toolBarView.staticTexts["6个标签页"].exists)

        // case: 检查关闭网页功能
        qe = tabsContainer.descendants(matching: .button).matching(identifier: "Close")
        print("qe:\(qe.count)")
        await qe.allElementsBoundByAccessibilityElement.reversed().asyncForEach { e in
            e.forceTapElement()
            print("qe: forceTapElement")
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        try? await Task.sleep(nanoseconds: 300_000_000)
        shiftButton.forceTapElement()
        XCTAssert(toolBarView.staticTexts["1个标签页"].exists)
        
        
        // case: 点击完成按钮功能
        let doneButton = toolBarView.descendants(matching: .button).matching(identifier: "done").firstMatch
        doneButton.forceTapElement()
        // tabsContainer:
        XCTAssert(tabsContainer.descendants(matching: .other).matching(identifier: "TabPageView").firstMatch.waitForExistence(timeout: 0.5))
        await checkTrue(wait: 0.5) { tabsContainer.descendants(matching: .other).matching(identifier: "AddressBar").firstMatch.isHittable }
        // toolbar:
        XCTAssertFalse(toolBarView.descendants(matching: .button).matching(identifier: "add").firstMatch.waitForExistence(timeout: 0.5))
        XCTAssert(toolBarView.descendants(matching: .staticText).allElementsBoundByAccessibilityElement.count == 0)
        XCTAssertFalse(toolBarView.descendants(matching: .button).matching(identifier: "done").firstMatch.waitForExistence(timeout: 0.5))
    }
    
    /*
        测试快捷键添加功能
     */
    @MainActor
    func test_webBrowser_toolBar_shortcut() async throws {
        await launchWebBrowser()
        
        // 加载一个新的网页
        await doLoadWebSite(url: "https://www.baidu.com")
        
        // 快捷键可点击
        XCTAssert(shortcutButton.isEnabled)
        
        // 点击添加快捷键
        shortcutButton.forceTapElement()
        
        //检查快捷键
        XCTAssert(app.staticTexts["百度一下"].waitForExistence(timeout: 0.3))
    }
    
    /*
        测试更多
     */
    @MainActor
    func test_webBrowser_toolBar_more_show_hide() async throws {
        await launchWebBrowser()

        // morepicker正常为隐藏
        XCTAssertFalse(moreSegmentedControl.isHittable)

        // 点击more: show
        moreButton.forceTapElement()
        XCTAssert(moreSegmentedControl.isHittable)

        // 点击more: hide
        doCloseMorePicker()
        XCTAssertFalse(moreSegmentedControl.isHittable)
    }
    
    @MainActor
    func test_webBrowser_toolBar_more_picker_UI() async throws {
        await launchWebBrowser()
        
        func checkTwoLayout() async {
            XCTAssert(moreSegmentedControl.buttons.allElementsBoundByAccessibilityElement.count == 2)
            // 默认选中第一个（bookmarksView）
            XCTAssert(moreSegmentedControl.buttons.firstMatch.isSelected)
            // 显示bookmarksView，隐藏historysView
            XCTAssert(bookmarksView.exists)
            XCTAssertFalse(historysView.exists)
            // 切换segment
            moreSegmentedControl.buttons.element(boundBy: 1).tap()
            await delay(0.3)
            // 显示historysView，隐藏bookmarksView
            XCTAssertFalse(bookmarksView.exists)
            XCTAssert(historysView.exists)
        }
        
        func checkThreeLayout() async {
            XCTAssert(moreSegmentedControl.buttons.allElementsBoundByAccessibilityElement.count == 3)
            // 默认选中第三个（historysView）
            XCTAssert(moreSegmentedControl.buttons.element(boundBy: 2).isSelected)
            // 显示historysView，隐藏menuView，bookmarksView
            XCTAssertFalse(menuView.exists)
            XCTAssertFalse(bookmarksView.exists)
            XCTAssert(historysView.exists)
            // 切换segment
            moreSegmentedControl.buttons.element(boundBy: 0).tap()
            await delay(0.3)
            // 显示menuView，隐藏bookmarksView，historysView
            XCTAssert(menuView.exists)
            XCTAssertFalse(bookmarksView.exists)
            XCTAssertFalse(historysView.exists)
            // 切换segment
            moreSegmentedControl.buttons.element(boundBy: 1).tap()
            // 显示bookmarksView，隐藏menuView，historysView
            XCTAssertFalse(menuView.exists)
            XCTAssert(bookmarksView.exists)
            XCTAssertFalse(historysView.exists)
        }
        
        // 2栏布局
        await closeAllWebSites()
        moreButton.forceTapElement()
        await delay(0.3)
        await checkTwoLayout()
        doCloseMorePicker()
        
        // 3栏布局
        await doLoadWebSite(url: "https://www.baidu.com")
        moreButton.forceTapElement()
        await delay(0.3)
        await checkThreeLayout()
        doCloseMorePicker()
    }
    
    @MainActor
    func test_webBrowser_toolBar_more_picker_menu() async throws {
        await launchWebBrowser()
        await closeAllWebSites()
        await doLoadWebSite(url: "https://www.baidu.com")
        moreButton.forceTapElement()
        await deleteAllBookmarksIfNeed()
        tapMoreSegmentedControl(0)

        func checkUI() {
            XCTAssert(morePicker.buttons["添加书签"].exists)
            XCTAssert(morePicker.staticTexts["无痕模式"].exists)
            XCTAssert(morePicker.switches["MenuView_TackToggle"].exists)
        }
        
        @MainActor
        func deleteAllBookmarksIfNeed() async {
            tapMoreSegmentedControl(1)
            guard bookmarksList.exists else { return }
            bookmarksView.swipeUp()
            await bookmarksList.staticTexts.allElementsBoundByAccessibilityElement.reversed().asyncForEach { e in
                e.swipeLeft()
                XCTAssert(morePicker.buttons["Delete"].exists)
                morePicker.buttons["Delete"].tap()
                await delay(0.3)
            }
        }
        
        
        func deleteBookmark(_ element: XCUIElement) {
            bookmarksView.swipeUp()
            element.swipeLeft()
            XCTAssert(morePicker.buttons["Delete"].exists)
            morePicker.buttons["Delete"].tap()
        }
        
        func deleteHistory(_ element: XCUIElement) {
            historysViewList.swipeUp()
            element.swipeLeft()
            XCTAssert(morePicker.buttons["Delete"].exists)
            morePicker.buttons["Delete"].tap()
        }
        
        func checkAddAndDeleteBookmark() {
            tapMoreSegmentedControl(0)
            
            let bookmark = morePicker.buttons["添加书签"]
            XCTAssert(bookmark.exists)
            bookmark.tap()
            // segment control选中bookmark
            tapMoreSegmentedControl(1)
            XCTAssert(bookmarksList.exists)
            XCTAssert(bookmarksList.staticTexts["百度一下"].exists)
            XCTAssertFalse(bookmarksEmpty.staticTexts["百度一下"].exists)
            deleteBookmark(bookmarksList.staticTexts["百度一下"])
            XCTAssertFalse(bookmarksList.exists)
            XCTAssert(bookmarksEmpty.exists)
        }
                
        func checkShare() {
            let share = morePicker.buttons["分享"]
            XCTAssert(share.exists)
            share.tap()
            let uiActivityContentView = app.navigationBars["UIActivityContentView"]
            XCTAssert(uiActivityContentView.waitForExistence(timeout: 0.3))
            XCTAssert(uiActivityContentView.otherElements["baidu.com"].waitForExistence(timeout: 0.3))
            XCTAssert(uiActivityContentView.buttons["Close"].waitForExistence(timeout: 0.5))
            uiActivityContentView.buttons["Close"].tap()
        }
        
        func checkTracknessMode() async {
            
            let keywordBaidu = "tackbaidu" + "-\(Int.random(in: 0...10000))"
            let keywordSogou = "tacksogou" + "-\(Int.random(in: 0...10000))"
            let keyword360so = "tack360so" + "-\(Int.random(in: 0...10000))"
            
            func toggleTackneeMode(_ on: Bool) {
                tapMoreSegmentedControl(0)
                let toggle = morePicker.switches["MenuView_TackToggle"]
                XCTAssert(toggle.exists)
                let toggleIsTrue = toggle.switcherValue
                if toggleIsTrue && !on {
                    toggle.switcherToggle()
                } else if (!toggleIsTrue && on) {
                    toggle.switcherToggle()
                }
            }
            
            func searchAndCheck(key: String, searcher :(String) async ->Void, expected: Bool) async {
                await searcher(key)
                moreButton.forceTapElement()
                tapMoreSegmentedControl(2)
                historysView.swipeDown()
                let predicate = NSPredicate(format: "label BEGINSWITH %@", key)
                let element = historysViewList.staticTexts.matching(predicate).firstMatch
                XCTAssert(element.exists == expected)
//                if expected {
//                    // 顺便测试下删除历史记录功能
//                    await delay(0.3)
//                    deleteHistory(element)
//                    XCTAssertFalse(element.waitForExistence(timeout: 0.3) == expected)
//                }
            }
            
            let toChecks: [(String, (String) async ->Void)] = [
                                                                (keywordBaidu, doBaiduSearch(_:)),
                                                                (keywordSogou, doSogouSearch(_:)),
                                                                // (keyword360so, doSo360Search(_:)), //360太过频繁调用会触发人机识别，先注释掉。
                                                                ]
            
            await toChecks.asyncForEach {
                toggleTackneeMode(false)
                doCloseMorePicker()
                await delay(0.3)
                await searchAndCheck(key: $0.0, searcher: $0.1, expected: true)
                
                await delay(0.3)
                
                toggleTackneeMode(true)
                doCloseMorePicker()
                await delay(0.3)
                await searchAndCheck(key: $0.0 + "-", searcher: $0.1, expected: false)
            }
        }
                
        checkUI()
        checkShare()
        checkAddAndDeleteBookmark()
        await checkTracknessMode()
    }
    
}
