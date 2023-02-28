import { BarcodeScanner } from '@capacitor-community/barcode-scanner';
import { CAMERA_DIRECTION, SUPPORTED_FORMAT } from './types';

/**
 *  准备扫描
 * @param targetedFormats 扫描文本类型
 * @param cameraDirection 摄像头位置
 */
const prepare = async (
  targetedFormats: SUPPORTED_FORMAT = SUPPORTED_FORMAT.QR_CODE,
  cameraDirection: CAMERA_DIRECTION = CAMERA_DIRECTION.BACK
) => {
  return await BarcodeScanner.prepare({
    targetedFormats: [targetedFormats],
    cameraDirection: cameraDirection,
  });
};

/**
 *  开始扫描
 * @param targetedFormats 扫描文本类型
 * @param cameraDirection 摄像头位置
 */
const startScan = async (
  targetedFormats: SUPPORTED_FORMAT = SUPPORTED_FORMAT.QR_CODE,
  cameraDirection: CAMERA_DIRECTION = CAMERA_DIRECTION.BACK
) => {
  const scanResult = await BarcodeScanner.startScan({
    targetedFormats: [targetedFormats],
    cameraDirection: cameraDirection,
  });
  if (scanResult.hasContent) {
    return scanResult.content;
  }
};

/**
 * 暂停扫描
 */
const pauseScanning = async () => {
  return await BarcodeScanner.pauseScanning();
};

/**
 * 恢复扫描
 */
const resumeScanning = async () => {
  return await BarcodeScanner.resumeScanning();
};

/**
 *  停止扫描
 * @param forceStopScan 是否强制停止扫描
 */
const stopScan = async (forceStopScan?: boolean) => {
  return await BarcodeScanner.stopScan({ resolveScan: forceStopScan });
};

/**
 *  检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限(设置)
 * @param forceCheck 是否强制检查权限
 */
const checkCameraPermission = async (
  forceCheck?: boolean,
  beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
) => {
  const permission = await BarcodeScanner.checkPermission({
    force: forceCheck,
  });
  // 如果是请求，那么直接返回结果
  if (permission.asked) {
    return permission.granted === true;
  }
  if (permission.granted) {
    return permission.granted;
  }
  // 打开配置请求权限
  if (permission.denied) {
    // 打开配置请求权限前置操作
    if (beforeOpenPermissionSettings) {
      const isOpenPermissionSettings = await beforeOpenPermissionSettings();
      if (isOpenPermissionSettings) {
        await BarcodeScanner.openAppSettings();
      }
    }
  }
  return false;
};

/**
 * 打开/关闭手电筒
 */
const toggleTorch = async () => {
  return await BarcodeScanner.toggleTorch();
};

/**
 * 手电筒状态
 */
const getTorchState = async () => {
  const torchState = await BarcodeScanner.getTorchState();
  return torchState.isEnabled;
};

/**
 * 隐藏webview背景
 */
const hideBackground = async () => {
  return await BarcodeScanner.hideBackground();
};

/**
 * 显示webview背景
 */
const showBackground = async () => {
  return await BarcodeScanner.showBackground();
};

export default {
  prepare,
  startScan,
  pauseScanning,
  resumeScanning,
  stopScan,
  checkCameraPermission,
  toggleTorch,
  getTorchState,
  hideBackground,
  showBackground,
};
