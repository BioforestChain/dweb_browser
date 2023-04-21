import { Toast } from '@capacitor/toast';

/**
 * toast信息显示
 * @param message 消息
 * @param duration 时长 'long' | 'short'
 * @returns
 */
const show = async (message: string, duration: 'long' | 'short' = 'long') => {
  return await Toast.show({ text: message, duration: duration, position: 'bottom' });
};
export default {
  show,
};
