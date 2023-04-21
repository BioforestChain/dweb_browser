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
  console.log('prepare', targetedFormats, cameraDirection);
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
  console.log('startScan', targetedFormats, cameraDirection);
  const scanContent = sessionStorage.getItem('fake-scan-result');
  if (scanContent) {
    return scanContent;
  }
};

/**
 * 暂停扫描
 */
const pauseScanning = async () => {
  console.log('pauseScanning');
};

/**
 * 恢复扫描
 */
const resumeScanning = async () => {
  console.log('resumeScanning');
};

/**
 *  停止扫描
 * @param forceStopScan 是否强制停止扫描
 */
const stopScan = async (forceStopScan?: boolean) => {
  console.log('stopScan', forceStopScan);
};

/**
 * 检查权限
 */
const checkCameraPermission = async (
  forceCheck?: boolean,
  beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
) => {
  console.log('checkCameraPermission', forceCheck);
  // 开发模式下模拟请求打开权限设置中心
  if (forceCheck) {
    const LS_KEY = 'permission';
    const P_NAME = 'camera';
    // 从session中获取是否请求过权限
    const permissionList = (localStorage.getItem(LS_KEY) ?? '').split('|');
    if (permissionList.includes(P_NAME)) {
      return true;
    }
    if (beforeOpenPermissionSettings === undefined) {
      return false;
    }
    const canOpenSettings = await beforeOpenPermissionSettings();
    if (canOpenSettings) {
      permissionList.push(P_NAME);
      localStorage.setItem(LS_KEY, permissionList.join('|'));
    }
    return canOpenSettings;
  }
  return true;
};

/**
 * 打开/关闭手电筒
 */
const toggleTorch = async () => {
  console.log('toggleTorch');
};

/**
 * 手电筒状态
 */
const getTorchState = async () => {
  console.log('getTorchState');
  // 开发模式下手电筒默认都是打开方式
  return true;
};

/**
 * 隐藏webview背景
 */
const hideBackground = async () => {
  console.log('hideBackground');
};

/**
 * 显示webview背景
 */
const showBackground = async () => {
  console.log('showBackground');
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
