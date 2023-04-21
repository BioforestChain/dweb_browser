import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import type { $TakePhotoOptions } from './types';

/**
 *  拍摄相片
 * options 相片选项
 */
const takePhoto = async (options: $TakePhotoOptions) => {
  return await Camera.getPhoto({
    quality: options.quality,
    allowEditing: false,
    resultType: options.resultType,
    saveToGallery: options.saveToGallery,
    source: CameraSource.Camera,
    direction: options.direction,
  });
};

/**
 * 从图库选取单张相片
 * @param quality 图片质量
 * @returns
 */
const pickPhoto = async (quality: number = 90) => {
  return await Camera.getPhoto({
    quality: quality,
    allowEditing: false,
    resultType: CameraResultType.Uri,
    source: CameraSource.Photos,
  });
};

/**
 * 从图库选取多张相片
 * @param quality 图片质量
 * @returns
 */
const pickPhotos = async (quality: number = 90) => {
  return await Camera.pickImages({
    quality: quality,
  });
};
export default {
  takePhoto,
  pickPhoto,
  pickPhotos,
};
