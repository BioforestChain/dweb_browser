import type { ShareOptions } from '@capacitor/share';

export type $Share = typeof import('./share.dev')['default'];
/**
 * 分享options
 */
export interface $ShareOptions extends ShareOptions {
  /** 图片dataUrl */
  imageData?: string;
}
