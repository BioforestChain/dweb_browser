export interface ShortcutOption {
  /**标题 */
  title: string;
  /**传递给应用的数据 */
  data: string;
  /**应用icon,不传递默认使用appIcon */
  icon: Uint8Array | null;
}
