import { regDevGlobal } from '@bnqkl/framework/environments';
import { default as Keyboard } from './keyboard.dev';
regDevGlobal('keyboard', Keyboard, true);

export * from './types';
export { Keyboard };
