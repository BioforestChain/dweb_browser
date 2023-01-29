//
//  LocationManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/18.
//

import UIKit
import CoreLocation

class LocationManager: NSObject {
    
    lazy private var manager: CLLocationManager = {
        let manager = CLLocationManager()
        return manager
    }()
    
    lazy private var gecoder: CLGeocoder = {
        let coder = CLGeocoder()
        return coder
    }()
}

extension LocationManager {
    
    func setUpManager() {
        //默认情况下 位置改变时locationManager就调用一次代理，设置distanceFilter可以实现 位置改变超出一定范围后才调用
        manager.distanceFilter = 300
        //精度
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.delegate = self
        //允许后台定位
//        manager.allowsBackgroundLocationUpdates = true
        manager.requestWhenInUseAuthorization()
        manager.requestAlwaysAuthorization() //后台定位且不会出现大蓝条
    }
    
    func startLocation() {
        if CLLocationManager.locationServicesEnabled() {
            manager.startUpdatingLocation()
        }
    }
}

extension LocationManager: CLLocationManagerDelegate {
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            gecoder.reverseGeocodeLocation(location) { mark, error in
                if let placeMark = mark?.first {
                    //国家。城市。街道
                    print(placeMark.country, placeMark.name, placeMark.locality)
                }
            }
        }
        manager.stopUpdatingLocation()
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("定位失败")
        manager.stopUpdatingLocation()
    }
}
