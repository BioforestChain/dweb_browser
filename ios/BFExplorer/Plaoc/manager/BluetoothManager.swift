//
//  BluetoothManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/14.
//

import UIKit
import CoreBluetooth

class BluetoothManager: NSObject {

    var bleManager: CBCentralManager? //中心设备，连接硬件的设备
    var peripheral: CBPeripheral? //当前连接的设备
    var writeChar: CBCharacteristic? // 发送数据的特征值
    var blueName: String = ""
    
    override init() {
        super.init()
        bleManager = CBCentralManager(delegate: self, queue: nil, options: nil)
        
    }
    
    //扫描设备的方法
    func scanForPeripheralsWithServices(serviceUUIDS: [CBUUID]?, options: [String:AnyObject]?) {
        bleManager?.scanForPeripherals(withServices: serviceUUIDS, options: options)
    }
    
    //停止扫描
    func stopScan() {
        bleManager?.stopScan()
    }
    
    //写数据
    func writeToPerapheral(data: Data) {
        guard writeChar != nil else { return }
        peripheral?.writeValue(data, for: writeChar!, type: .withResponse)
    }
    
    //连接某个设备
    func requestConnectPeripheral(model: CBPeripheral) {
        if model.state != .connected {
            bleManager?.connect(model)
        }
    }
    
}

extension BluetoothManager: CBCentralManagerDelegate {
    
    //检测app设备是否支持蓝牙
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        
        switch central.state {
        case .poweredOn:
            print("蓝牙打开")
            bleManager?.scanForPeripherals(withServices: nil, options: nil)
        case .unauthorized:
            print("没有蓝牙功能")
        case .poweredOff:
            print("蓝牙关闭")
        default:
            print("未知状态")
        }
    }
    
    //开始扫描之后会扫描到蓝牙设备，扫描之后走这代理。 中心管理器扫描到了设备
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        //iOS目前不提供蓝牙设备的UUID获取，在这理通过蓝牙名称判断
        guard peripheral.name != nil else { return }  //, peripheral.name!.contains(blueName)
        bleManager?.stopScan()
        self.peripheral = peripheral
        bleManager?.connect(peripheral)
        print(peripheral.name)
        print("=====")
    }
    
    //连接外设成功，开始发现服务
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        bleManager?.stopScan()
        peripheral.delegate = self
        peripheral.discoverServices(nil)
        self.peripheral = peripheral
    }
    //连接失败
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        
    }
    //连接丢失
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        self.peripheral = nil
        self.writeChar = nil
        
        //如果需要重新扫描
        bleManager?.scanForPeripherals(withServices: nil, options: nil)
    }
}

extension BluetoothManager: CBPeripheralDelegate {
    
    //匹配对应的服务UUID
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard error == nil else { return }
        guard peripheral.services != nil else { return }
        for service in peripheral.services! {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }
    
    //服务下的特征
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard error == nil else { return }
        guard service.characteristics != nil else { return }
        for character in service.characteristics! {
            switch character.uuid.description {
            case "具体的特征":
                //订阅特征值，订阅成功后，后续所有值的变化都会自动通知
                peripheral.setNotifyValue(true, for: character)
            case "xxxxxx":
                //读取特征值，只能读到一次
                peripheral.readValue(for: character)
            default:
                print(character.uuid.description)
                print("扫描到其他特征")
                peripheral.setNotifyValue(true, for: character)
            }
        }
    }
    
    //特征的订阅状态体发生变化
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        guard error == nil else { return }
        //发送
        
    }
    
    //获取外设发来的数据
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard error == nil else { return }
        switch characteristic.uuid.uuidString {
        case "具体的特征":
            print("收到特征值的变化")
        default:
            print("扫描到其他特征")
        }
    }
    
    //检测写数据是否成功
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        guard error == nil else { return }
        print("发送成功")
    }
}
