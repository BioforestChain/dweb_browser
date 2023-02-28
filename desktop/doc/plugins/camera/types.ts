import type { CameraDirection, CameraResultType } from '@capacitor/camera';
export { GalleryPhoto as $GalleryPhoto, GalleryPhotos as $GalleryPhotos, Photo as $Photo } from '@capacitor/camera';

/** 拍摄相片选项 */
export interface $TakePhotoOptions {
  /** 相片质量 */
  quality?: number;
  /** 成功或者到相片后的返回模式 */
  resultType: CameraResultType;
  /** 是否保存到图库 */
  saveToGallery?: boolean;
  /** 摄像头方向  */
  direction?: CameraDirection;
}

export type $Camera = typeof import('./camera.capacitor')['default'];
