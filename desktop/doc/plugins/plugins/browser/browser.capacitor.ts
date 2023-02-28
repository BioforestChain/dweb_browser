import { Browser } from '@capacitor/browser';
import type { $OpenOptions } from './types';

/**
 * Open a page with the specified options.
 */
const open = async (options: $OpenOptions) => {
  return Browser.open(options);
};

export default { open };
