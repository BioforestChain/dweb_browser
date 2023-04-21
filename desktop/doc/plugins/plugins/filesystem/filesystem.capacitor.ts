import { BarcodeScanner } from '@capacitor-community/barcode-scanner';
import { Filesystem } from '@capacitor/filesystem';
import {
  $AppendFileOptions,
  $DeleteFileOptions,
  $ReadFileOptions,
  $ReadFileResult,
  $StatOptions,
  $WriteFileOptions,
  DIRECTORY,
} from './types';

/** 检查并请求文件系统权限 */
const checkAndRequestPermissions = async (beforeOpenPermissionSettings?: () => boolean | Promise<boolean>) => {
  /// 检查文件系统权限
  let permissionStatus = await Filesystem.checkPermissions();
  if (permissionStatus.publicStorage === 'granted') {
    return true;
  }
  // 是否是第一次请求权限系统
  const firstRequest = localStorage.getItem('fs-permission-first-request');
  /// 请求文件系统权限
  permissionStatus = await Filesystem.requestPermissions();
  if (firstRequest == undefined) {
    localStorage.setItem('fs-permission-first-request', 'true');
  }
  if (permissionStatus.publicStorage === 'granted') {
    return true;
  }
  if (firstRequest != undefined && beforeOpenPermissionSettings) {
    const isOpenPermissionSettings = await beforeOpenPermissionSettings();
    if (isOpenPermissionSettings) {
      await BarcodeScanner.openAppSettings();
    }
  }
  return false;
};

/**
 * 读取文件
 * @param options 参数信息
 * @returns
 */
const _readFile = (options: $ReadFileOptions): Promise<$ReadFileResult> => {
  return Filesystem.readFile(options);
};

/**
 * 读取相片
 * @param path 路径
 * @param directory 文件目录
 * @returns
 */
const readPhotoFile = async (path?: string, directory?: DIRECTORY) => {
  return _readFile({
    path: path ?? `/Pictures/COT/${Date.now()}.png`,
    directory: directory ?? DIRECTORY.ExternalStorage,
  });
};

/**
 *  读取文件
 * @param options 参数信息
| * @returns
 */
const readDocumentFile = async (options: $ReadFileOptions) => {
  return _readFile(options);
};

/**
 *  获取文件信息
 * @param options 参数信息
| * @returns
 */
const stat = async (options: $StatOptions) => {
  return await Filesystem.stat(options);
};

/**
 * 写入文件
 * @param options 参数信息
 * @param checkPrimission 是否检查权限
 * @returns
 */
const _writeFile = async (
  options: $WriteFileOptions,
  checkPrimission: boolean = true,
  beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
) => {
  if (checkPrimission) {
    const hasPermisstion = await checkAndRequestPermissions(beforeOpenPermissionSettings);
    if (hasPermisstion === false) {
      throw new Error('no storage permission');
    }
  }
  return Filesystem.writeFile({ recursive: true, ...options });
};

/**
 * 追加文件数据
 * @param options 参数信息
 * @returns
 */
const _appendFile = async (options: $AppendFileOptions) => {
  return Filesystem.appendFile(options);
};

/**
 * 保存相片
 * @param imageData 图片数据
 * @param path 路径
 * @param directory 文件目录
 * @param checkPrimission 是否检查权限
 * @returns
 */
const writePhotoFile = async (
  imageData: string,
  path?: string,
  directory?: DIRECTORY,
  checkPrimission: boolean = true,
  beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
) => {
  return _writeFile(
    {
      path: path ?? `/Pictures/COT/${Date.now()}.png`,
      data: imageData,
      directory: directory ?? DIRECTORY.ExternalStorage,
    },
    checkPrimission,
    beforeOpenPermissionSettings
  );
};

/**
 *  写入文件
 * @param options 参数信息
 * @param checkPrimission 是否检查权限
 * @returns
 */
const writeDocumentFile = async (
  options: $WriteFileOptions,
  checkPrimission: boolean = true,
  beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
) => {
  return _writeFile(options, checkPrimission, beforeOpenPermissionSettings);
};

/**
 *  追加文件数据
 * @param options 参数信息
 * @returns
 */
const appendDocumentFile = async (options: $WriteFileOptions) => {
  return _appendFile(options);
};

/**
 * 删除文件
 * @returns
 */
const deleteFile = async (options: $DeleteFileOptions) => {
  return Filesystem.deleteFile(options);
};

export default {
  readDocumentFile,
  readPhotoFile,
  writePhotoFile,
  writeDocumentFile,
  deleteFile,
  appendDocumentFile,
  stat,
  checkAndRequestPermissions,
};
