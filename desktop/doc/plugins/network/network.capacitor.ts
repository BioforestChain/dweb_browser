import { $once } from '@bnqkl/framework/helpers';
import { ConnectionStatus, Network } from '@capacitor/network';
import { BehaviorSubject } from 'rxjs';
import type { $NetworkStatus } from './types';

/** 获取网络状态变化的管理器 */
export const getNetworkStatus$ = $once(async () => {
  const transform = (status: ConnectionStatus): $NetworkStatus => {
    return {
      online: status.connected,
      connectionType: status.connectionType,
    };
  };
  const network$ = new BehaviorSubject(transform(await Network.getStatus()));
  Network.addListener('networkStatusChange', (status) => {
    network$.next(transform(status));
  });
  return network$;
});

/** 获取网络是否在线 */
export const getOnlineStatus$ = $once(() => {
  const online$ = new BehaviorSubject(navigator.onLine ?? true); // 默认在线
  getNetworkStatus$().then((networkStatus$) => {
    networkStatus$.subscribe((status) => {
      online$.next(status.online);
    });
  });
  return online$;
});

export default {
  getNetworkStatus$,
  getOnlineStatus$,
};
