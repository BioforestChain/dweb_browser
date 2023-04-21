import { default as Toast } from './toast.capacitor';

/**
 * toast信息显示
 * @param message 消息
 * @param duration 时长 'long' | 'short'
 * @returns
 */
const show = async (message: string, duration: 'long' | 'short' = 'long') => {
  await Toast.show(message, duration);
  return console.log(message);
};
export default {
  show,
};
