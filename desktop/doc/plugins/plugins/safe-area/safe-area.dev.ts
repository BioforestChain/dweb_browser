/**
 * 模拟 env-safe-area-insets
 */
const getSafeAreaInsets = async () => {
  const get = (key: string, default_val: number) => {
    let val = default_val;
    const str = localStorage.getItem(key);
    if (str && Number.isFinite(str)) {
      val = Number.parseFloat(str);
    }
    return val;
  };
  const is_ios = isIOS();
  const safeArea = is_ios
    ? {
        top: 44,
        left: 10,
        bottom: 34,
        right: 10,
      }
    : {
        top: 24,
        left: 10,
        bottom: 13,
        right: 10,
      };

  return {
    top: get('env-safe-area-inset-top', safeArea.top),
    left: get('env-safe-area-inset-left', safeArea.left),
    bottom: get('env-safe-area-inset-bottom', safeArea.bottom),
    right: get('env-safe-area-inset-right', safeArea.right),
  };
};
import { isIOS } from '@bnqkl/framework/helpers';
import { default as SafeArea } from './safe-area.capacitor';
export default { ...SafeArea, getSafeAreaInsets };
