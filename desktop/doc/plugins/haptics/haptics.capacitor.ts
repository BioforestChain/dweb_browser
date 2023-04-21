import { Haptics, ImpactStyle, NotificationType } from '@capacitor/haptics';

/** 触碰轻质量的物体 */
const impactLight = () => Haptics.impact({ style: ImpactStyle.Light });
/** 警告分隔的振动通知 */
const notificationWarning = () => Haptics.notification({ type: NotificationType.Warning });
/** 单击手势的反馈振动 */
const vibrateClick = () => Haptics.vibrate({ duration: 1 });
/** 禁用手势的反馈振动,
 * 与 headShak 特效一致, 详见 ripple-button.animation.ts
 * headShak 是一段抖动特效, 前面抖动增强然后衰退
 * 这里只针对抖动增强阶段提供同步的振动反馈
 */
const vibrateDisabled = async () =>
  navigator.vibrate([
    // 初始触摸打击
    1,
    // 65ms时间点的打击
    63, 1,
    // 185ms时间点的打击
    119, 1,
    // 315ms时间点的打击
    129, 1,
  ]);
/** 双击手势的反馈振动 */
const vibrateDoubleClick = async () => navigator.vibrate([10, 1]);
/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
const vibrateHeavyClick = async () => navigator.vibrate([1, 100, 1, 1]);
/** 滴答 */
const vibrateTick = async () => navigator.vibrate([10, 999, 1, 1]);

export default {
  impactLight,
  notificationWarning,
  vibrateClick,
  vibrateDisabled,
  vibrateDoubleClick,
  vibrateHeavyClick,
  vibrateTick,
};
