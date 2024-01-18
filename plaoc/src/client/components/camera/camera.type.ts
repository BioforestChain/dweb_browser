export enum CameraSource {
  /**
   * Prompts the user to select either the photo album or take a photo.
   */
  Prompt = "PROMPT",
  /**
   * Take a new photo using the camera.
   */
  Camera = "CAMERA",
  /**
   * Pick an existing photo from the gallery or photo album.
   */
  Photos = "PHOTOS",
}
// 输入参数
export interface ImageOptions {
  /**
   * The quality of image to return as JPEG, from 0-100
   *
   * @since 2.0.0
   */
  quality?: number;
  /**
   * How the data should be returned. Currently, only 'Base64', 'DataUrl' or 'Uri' is supported
   *
   * @since 2.0.0
   */
  resultType?: CameraResultType;
  /**
   * Whether to save the photo to the gallery.
   * If the photo was picked from the gallery, it will only be saved if edited.
   * @default: false
   *
   * @since 2.0.0
   */
  saveToGallery?: boolean;
  /**
   * The source to get the photo from. By default this prompts the user to select
   * either the photo album or take a photo.
   * @default: CameraSource.Prompt
   *
   * @since 2.0.0
   */
  source?: CameraSource;
}
// 输出参数
export interface Photo {
  /**
   * The base64 encoded string representation of the image, if using CameraResultType.Base64.
   *
   * @since 2.0.0
   */
  base64String?: string;
  /**
   * If using CameraResultType.Uri, the path will contain a full,
   * platform-specific file URL that can be read later using the Filesystem API.
   *
   * @since 2.0.0
   */
  path?: string;
  /**
   * The format of the image, ex: jpeg, png, gif.
   *
   * iOS and Android only support jpeg.
   * Web supports jpeg and png. gif is only supported if using file input.
   *
   * @since 2.0.0
   */
  format: string;
  /**
   * Whether if the image was saved to the gallery or not.
   *
   * On Android and iOS, saving to the gallery can fail if the user didn't
   * grant the required permissions.
   * On Web there is no gallery, so always returns false.
   *
   * @since 2.0.0
   */
  saved: boolean;
}

export enum CameraResultType {
  Uri = "Uri",
  Base64 = "Base64" 
}

