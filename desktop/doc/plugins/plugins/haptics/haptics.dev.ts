export * from './haptics.capacitor';
export { default } from './haptics.capacitor';

navigator.vibrate = (pattern: VibratePattern) => {
  // eslint-disable-next-line no-restricted-syntax
  console.info('原生设备振动:', pattern);
  return true;
};
