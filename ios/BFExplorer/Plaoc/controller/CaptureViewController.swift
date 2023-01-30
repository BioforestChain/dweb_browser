//
//  CaptureViewController.swift
//  DWebBrowser
//
//  Created by mac on 2022/6/14.
//

import UIKit
import AVFoundation
import Photos

class CaptureViewController: UIViewController {
    
    //视频捕获会话。它是input和output的桥梁。它协调着intput到output的数据传输
    private let captureSession = AVCaptureSession()
    //视频输入设备
    private let videoDevice = AVCaptureDevice.default(for: .video)
    //音频输入设备
    private let audioDevice = AVCaptureDevice.default(for: .audio)
    //将捕获到的视频输出到文件
    private let fileOutput = AVCaptureMovieFileOutput()
    
    private var isRecording: Bool = false
    
    //开始、停止按钮
    var startButton, stopButton : UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        //添加视频、音频输入设备
        guard videoDevice != nil, audioDevice != nil else { return }
        if let videoInput = try? AVCaptureDeviceInput(device: videoDevice!) {
            captureSession.addInput(videoInput)
        }
        
        if let audioInput = try? AVCaptureDeviceInput(device: audioDevice!) {
            captureSession.addInput(audioInput)
        }
        
        //添加视频捕获输出
        captureSession.addOutput(fileOutput)
        
        //使用AVCaptureVideoPreviewLayer可以将摄像头的拍摄的实时画面显示在ViewController上
        let videoLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        videoLayer.frame = self.view.bounds
        videoLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
        self.view.layer.addSublayer(videoLayer)
        
        captureSession.startRunning()
    }
    
    //创建按钮
    func setupButton(){
        //创建开始按钮
        self.startButton = UIButton(frame: CGRect(x:0,y:0,width:120,height:50))
        self.startButton.backgroundColor = UIColor.red
        self.startButton.layer.masksToBounds = true
        self.startButton.setTitle("开始", for: .normal)
        self.startButton.layer.cornerRadius = 20.0
        self.startButton.layer.position = CGPoint(x:self.view.bounds.width/2 - 70,
                                                  y:self.view.bounds.height-50)
        self.startButton.addTarget(self, action: #selector(onClickStartButton(_:)),
                                   for: .touchUpInside)
        
        //创建停止按钮
        self.stopButton = UIButton(frame: CGRect(x:0,y:0,width:120,height:50))
        self.stopButton.backgroundColor = UIColor.gray
        self.stopButton.layer.masksToBounds = true
        self.stopButton.setTitle("停止", for: .normal)
        self.stopButton.layer.cornerRadius = 20.0
        
        self.stopButton.layer.position = CGPoint(x: self.view.bounds.width/2 + 70,
                                                 y:self.view.bounds.height-50)
        self.stopButton.addTarget(self, action: #selector(onClickStopButton(_:)),
                                  for: .touchUpInside)
        
        //添加按钮到视图上
        self.view.addSubview(self.startButton)
        self.view.addSubview(self.stopButton)
    }
}

extension CaptureViewController {
    
    //开始按钮点击，开始录像
    @objc func onClickStartButton(_ sender: UIButton){
        guard !isRecording else { return }
        let filePath = documentdir + "/temp.mp4"
        let fileUrl = URL(fileURLWithPath: filePath)
        
        //启动视频编码输出
        fileOutput.startRecording(to: fileUrl, recordingDelegate: self)
        
        isRecording = true
        //开始、结束按钮颜色改变
//        self.changeButtonColor(target: self.startButton, color: .gray)
//        self.changeButtonColor(target: self.stopButton, color: .red)
    }
    
    //停止按钮点击，停止录像
    @objc func onClickStopButton(_ sender: UIButton){
        guard isRecording else { return }
        fileOutput.stopRecording()
        isRecording = false
        //开始、结束按钮颜色改变
//        self.changeButtonColor(target: self.startButton, color: .gray)
//        self.changeButtonColor(target: self.stopButton, color: .red)
    }
}

extension CaptureViewController: AVCaptureFileOutputRecordingDelegate {
    
    //录像开始的代理方法
    func fileOutput(_ output: AVCaptureFileOutput, didStartRecordingTo fileURL: URL, from connections: [AVCaptureConnection]) {
        
    }
    //录像结束的代理方法
    func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
        
        var message: String!
        
        PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: outputFileURL)
        } completionHandler: { issuccess, error in
            if issuccess {
                message = "保存成功!"
            } else{
                message = "保存失败：\(error!.localizedDescription)"
            }
            DispatchQueue.main.async {
                //弹框
            }
        }
    }
}
