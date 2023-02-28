import { default as CapFilesystem } from './filesystem.capacitor';

/** 检查并请求文件系统权限 */
const checkAndRequestPermissions = async (beforeOpenPermissionSettings?: () => boolean | Promise<boolean>) => {
  console.log('checkAndRequestFileSystemPermissions', checkAndRequestPermissions);
  // 从session中获取是否请求过权限
  const permission = localStorage.getItem('permission') ?? '';
  const hasWritePermission = permission.includes('write');
  const hasReadPermission = permission.includes('read');
  if ((hasWritePermission && hasReadPermission) === false) {
    if (beforeOpenPermissionSettings) {
      const canOpenSettings = await beforeOpenPermissionSettings();
      if (canOpenSettings) {
        console.log('openPermissionSettings');
        const permissionList = permission.split('|').filter((item) => item);
        if (hasWritePermission === false) {
          permissionList.push('write');
        }
        if (hasReadPermission === false) {
          permissionList.push('read');
        }
        localStorage.setItem('permission', permissionList.join('|'));
      }
    }
    return false;
  }
  return true;
};

export default {
  readDocumentFile: CapFilesystem.readDocumentFile,
  readPhotoFile: CapFilesystem.readPhotoFile,
  writePhotoFile: CapFilesystem.writePhotoFile,
  writeDocumentFile: CapFilesystem.writeDocumentFile,
  deleteFile: CapFilesystem.deleteFile,
  appendDocumentFile: CapFilesystem.appendDocumentFile,
  stat: CapFilesystem.stat,
  checkAndRequestPermissions,
};
