//
//  DwebBrowserTests.swift
//  DwebBrowserTests
//
//  Created by instinct on 2024/2/27.
//  Copyright © 2024 orgName. All rights reserved.
//

import XCTest
@testable import DwebBrowser

final class DwebLifeStatusCenterRecordStatusEventTests: XCTestCase {
        
    func testStatusReduceNone() {
        
        DwebLifeStatusCenter.Record.Event.allCases.forEach { event in
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.none
            
            // Then:
            XCTAssertEqual(sut == event, false)
            
            // When:
            sut += event
            // Then:
            if case .didLaunched = event {
                XCTAssert(sut == .launched(false))
            } else if case .willTerminated = event {
                XCTAssert(sut == .terminate)
            } else {
                XCTAssert(sut == DwebLifeStatusCenter.Record.Status.none)
            }
        }
    }
    
    func testStatusReduceDidLaunchFalse() {
        DwebLifeStatusCenter.Record.Event.allCases.forEach { event in
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.launched(false)
            
            // Then:
            XCTAssertEqual(sut == event, event == .didLaunched)
            
            // When:
            sut += event
            // Then:
            if case .willTerminated = event {
                XCTAssertEqual(sut, .terminate)
            } else if case .didRended = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.launched(true))
            }  else if case .didActived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(false))
            }  else if case .didUnactived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(false))
            } else {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.launched(false))
            }
        }
    }
    
    func testStatusReduceDidLaunchTrue() {
                
        for event in DwebLifeStatusCenter.Record.Event.allCases {
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.launched(true)
            
            // Then:
            XCTAssertEqual(sut == event, event == .didRended)
            
            // when:
            if case .didLaunched = event {
                // Then
                continue // skip
            }
            // When:
            sut += event
            // Then:
            if case .willTerminated = event {
                XCTAssertEqual(sut, .terminate)
            } else if case .didActived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(true))
            } else if case .didUnactived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(true))
            } else {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.launched(true))
            }
        }
    }
    
    func testStatusReduceActiveFalse() {
        for event in DwebLifeStatusCenter.Record.Event.allCases {
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.actived(false)
            
            // Then:
            XCTAssertEqual(sut == event, event == .didActived)
            
            // when:
            if case .didLaunched = event {
                // Then
                continue // skip
            }
            // When:
            sut += event
            // Then:
            if case .willTerminated = event {
                XCTAssertEqual(sut, .terminate)
            }  else if case .didRended = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(true))
            } else if case .didUnactived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(false))
            } else {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(false))
            }
        }
    }
    
    func testStatusReduceActiveTrue() {
        for event in DwebLifeStatusCenter.Record.Event.allCases {
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.actived(true)
            
            // Then:
            XCTAssertEqual(sut == event, event == .didRended)
            
            // when:
            if case .didLaunched = event {
                // Then
                continue // skip
            }
            // When:
            sut += event
            // Then:
            if case .willTerminated = event {
                XCTAssertEqual(sut, .terminate)
            } else if case .didUnactived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(true))
            } else {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(true))
            }
        }
    }
    
    func testStatusReduceUnactiveFalse() {
        for event in DwebLifeStatusCenter.Record.Event.allCases {
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.unactived(false)
            
            // Then:
            XCTAssertEqual(sut == event, event == .didUnactived)
            
            // when:
            if case .didLaunched = event {
                // Then
                continue // skip
            }
            // When:
            sut += event
            // Then:
            if case .willTerminated = event {
                XCTAssertEqual(sut, .terminate)
            } else if case .didRended = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(true))
            } else if case .didActived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(false))
            } else {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(false))
            }
        }
    }
    
    func testStatusReduceUnactiveTrue() {
        for event in DwebLifeStatusCenter.Record.Event.allCases {
            
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.unactived(true)
            
            // Then:
            XCTAssertEqual(sut == event, event == .didRended)
            
            // when:
            if case .didLaunched = event {
                // Then
                continue // skip
            }
            // When:
            sut += event
            // Then:
            if case .willTerminated = event {
                XCTAssertEqual(sut, .terminate)
            } else if case .didActived = event {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.actived(true))
            } else {
                XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.unactived(true))
            }
        }
    }
    
    func testStatusReduceTerminate() {
        DwebLifeStatusCenter.Record.Event.allCases.forEach { event in
            // Given:
            var sut = DwebLifeStatusCenter.Record.Status.terminate
            
            // Then:
            XCTAssertEqual(sut == event, event == .willTerminated)
            
            // When:
            sut += event
            // Then:
            XCTAssertEqual(sut, DwebLifeStatusCenter.Record.Status.terminate)
        }
    }
}
