export declare const createSignal: <CB extends $Callback<any[], unknown> = $Callback<[], unknown>>(autoStart?: boolean) => Signal<CB>;
export declare class Signal<CB extends $Callback<any[]> = $Callback> {
    constructor(autoStart?: boolean);
    private _cbs;
    private _started;
    private get _cachedEmits();
    start: () => void;
    listen: (cb: CB) => $OffListener;
    emit: (...args: Parameters<CB>) => void;
    private _emit;
    clear: () => void;
}
export type $Callback<ARGS extends unknown[] = [], RETURN = unknown> = (...args: ARGS) => RETURN;
export type $OffListener = () => boolean;
