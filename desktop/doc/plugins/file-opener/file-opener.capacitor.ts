import { FileOpener } from '@capacitor-community/file-opener';

/**
 *  打开文件
 * filePath 文件路径
 */
const open = async (filePath: string) => {
  return await FileOpener.open({ filePath, openWithDefault: true });
};

export default {
  open,
};
