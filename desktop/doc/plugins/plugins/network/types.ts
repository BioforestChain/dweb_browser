export type $NetworkStatus = {
  online: boolean;
  connectionType: 'wifi' | 'cellular' | 'none' | 'unknown';
};
export type $Netowrk = typeof import('./network.capacitor')['default'];
