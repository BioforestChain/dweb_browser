import { DIRECTORY, Filesystem } from '@bnqkl/framework/plugins/filesystem';
import { Share } from '@capacitor/share';
import type { $ShareOptions } from './types';

/**
 * 分享
 * @param options
 * @returns
 */
const share = async (options: $ShareOptions) => {
  const canShare = await Share.canShare();
  if (canShare.value) {
    // 是否为文件分享
    if (options.imageData) {
      const imageData = options.imageData;
      try {
        const fileType = imageData.split(',')[0].split(';')[0].split('/')[1];
        const writeFileResult = await Filesystem.writePhotoFile(
          imageData,
          `/Share/${Date.now()}.${fileType}`,
          DIRECTORY.External,
          false
        );
        if (writeFileResult) {
          delete options.imageData;
          options.url = writeFileResult.uri;
        }
        return await Share.share(options);
      } catch (err) {
        throw new Error(err as string);
      } finally {
        // 将临时保存的文件删除掉
        if (options.url) {
          Filesystem.deleteFile({
            path: options.url,
          });
        }
      }
    }
  }
  throw new Error('your device does not support the sharing function');
};

export default { share };
