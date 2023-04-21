import { SafeArea } from 'capacitor-plugin-safe-area';
import type { $SafeArea } from './types';
/**
 * 获取 env-safe-area-insets
 */
const getSafeAreaInsets = async (): Promise<$SafeArea> => {
  return (await SafeArea.getSafeAreaInsets()).insets;
};

export default {
  getSafeAreaInsets,
};
