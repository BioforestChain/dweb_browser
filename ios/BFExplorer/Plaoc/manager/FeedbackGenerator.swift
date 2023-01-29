//
//  FeedbackGenerator.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/16.
//

import UIKit
import CoreHaptics
import AudioToolbox

class FeedbackGenerator: NSObject {
    
    enum VibrateType: String {
        case CLICK = "1"
        case DOUBLE_CLICK = "10,1"
        case HEAVY_CLICK = "1,100,1,1"
        case TICK = "10,999,1,1"
        case DISABLED = "1,63,1,119,1,129,1"
    }

    
    static func notificationFeedbackGenerator(style: UINotificationFeedbackGenerator.FeedbackType) {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(style)
    }
    
    static func impactFeedbackGenerator(style: UIImpactFeedbackGenerator.FeedbackStyle) {
        let gene = UIImpactFeedbackGenerator(style: style)
        gene.impactOccurred()
    }
    
    static func selectionFeedbackGenerator() {
        let gene = UISelectionFeedbackGenerator()
        gene.selectionChanged()
    }
    
    static func vibrate(_ duration: Double) {
        if CHHapticEngine.capabilitiesForHardware().supportsHaptics {
            do {
                let engine = try CHHapticEngine()
                try engine.start()
                engine.resetHandler = {
                    do {
                        try engine.start()
                    } catch {
                        AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
                    }
                }
                let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: 1.0)
                let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: 1.0)
                
                let continuousEvent = CHHapticEvent(eventType: .hapticContinuous, parameters: [intensity,sharpness], relativeTime: 0.0, duration: Double(duration/100))
                let pattern = try CHHapticPattern(events: [continuousEvent], parameters: [])
                let player = try engine.makePlayer(with: pattern)
                try player.start(atTime: 0)
            } catch {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        } else {
            AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
        }
    }
    
    static func vibrate0(durationArr: [Double]) {
        if CHHapticEngine.capabilitiesForHardware().supportsHaptics {
            do {
                let engine = try CHHapticEngine()
                try engine.start()
                engine.resetHandler = {
                    do {
                        try engine.start()
                    } catch {
                        AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
                    }
                }
                let kickParams = [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: 1.0),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: 1.0)
                ]
                let rhythmParams = [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: 0.0),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.0)
                ]

                var events: [CHHapticEvent] = []
                var relativeTime = 0.0
                
                durationArr.enumerated().forEach { index, duration in
                    print("index: \(index) duration: \(duration)")
                    var continuousEvent: CHHapticEvent
                    if index % 2 == 0 {
                        continuousEvent = CHHapticEvent(eventType: .hapticContinuous, parameters: rhythmParams, relativeTime: relativeTime, duration: Double(duration)/1000)
                    } else {
                        continuousEvent = CHHapticEvent(eventType: .hapticContinuous, parameters: kickParams, relativeTime: relativeTime, duration: Double(duration)/1000)
                    }
                    
                    
                    print("relativeTime: \(relativeTime) duration: \(Double(duration)/1000)")
                    relativeTime += Double(duration)/1000
                    
                    events.append(continuousEvent)
                }

                let pattern = try CHHapticPattern(events: events, parameters: [])
                let player = try engine.makePlayer(with: pattern)
                try player.start(atTime: 0)
            } catch {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        } else {
            AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
        }
    }
    
    static func vibrate(durationArr: [Double]) {
        if CHHapticEngine.capabilitiesForHardware().supportsHaptics {
            do {
                let engine = try CHHapticEngine()
                try engine.start()
                engine.resetHandler = {
                    do {
                        try engine.start()
                    } catch {
                        AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
                    }
                }
              
                var events: [CHHapticEvent] = []
                var relativeTime = 0.0
                
                durationArr.enumerated().forEach { index, duration in
                    print("index: \(index) duration: \(duration)")
                    if index % 2 == 0 {
                        
                        let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: 0.5)
                        let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.6)
                        let continuousEvent = CHHapticEvent(eventType: .hapticContinuous, parameters: [intensity, sharpness], relativeTime: relativeTime, duration: max(0.01, Double(duration)/1000))
                        events.append(continuousEvent)
                    }

                    print("relativeTime: \(relativeTime) duration: \(Double(duration)/1000)")
                    relativeTime += Double(duration)/1000

                }
//                for i in stride(from: 0, to: 1, by: 0.1) {
//                    let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: Float(i))
//                    let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: Float(i))
//                    let event = CHHapticEvent(eventType: .hapticTransient, parameters: [intensity, sharpness], relativeTime: i)
//                    events.append(event)
//                }
//
//                for i in stride(from: 0, to: 1, by: 0.1) {
//                    let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: Float(1 - i))
//                    let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: Float(1 - i))
//                    let event = CHHapticEvent(eventType: .hapticTransient, parameters: [intensity, sharpness], relativeTime: 1 + i)
//                    events.append(event)
//                }
//
                
                let pattern = try CHHapticPattern(events: events, parameters: [])
                let player = try engine.makePlayer(with: pattern)
                try player.start(atTime: 0)
                
           
            } catch {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        } else {
            AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
        }
    }
    
    static func vibratePreset(type: String) {
        var durationStr: String
        switch(type) {
        case "CLICK":
            durationStr = VibrateType.CLICK.rawValue
        case "DOUBLE_CLICK":
            durationStr = VibrateType.DOUBLE_CLICK.rawValue
        case "HEAVY_CLICK":
            durationStr = VibrateType.HEAVY_CLICK.rawValue
        case "TICK":
            durationStr = VibrateType.TICK.rawValue
        case "DISABLED":
            durationStr = VibrateType.DISABLED.rawValue
        default:
            print("HapticsVibratePreset param error")
            durationStr = ""
        }
        
        if durationStr.count > 0 {
            let durationArr = durationStr.split(separator: ",").map { Double($0) ?? 1.0 }
            vibrate(durationArr: durationArr)
        }
    }
}

