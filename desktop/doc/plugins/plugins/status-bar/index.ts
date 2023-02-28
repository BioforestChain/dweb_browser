import { regDevGlobal } from '@bnqkl/framework/environments';
import { default as StatusBar } from './status-bar.dev';
regDevGlobal('statusBar', StatusBar);

export * from './types';
export { StatusBar };
