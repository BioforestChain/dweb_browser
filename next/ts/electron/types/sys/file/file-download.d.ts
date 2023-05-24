/**
 *
 * @param url
 * @param progress_callback 进程过程中的回调
 * @param target 文件保存的地址
 * @returns
 */
export declare function download(url: string, app_id: string, progress_callback: $ProgressCallback, appInfo: string): Promise<Boolean>;
export interface $State {
    percent: number;
    speed: number;
    size: {
        total: number;
        transferred: number;
    };
    time: {
        elapsed: number;
        remaining: number;
    };
}
export interface $ProgressCallback {
    (state: $State): void;
}
export interface $Resolve<T> {
    (value: T): void;
}
export interface $Reject<T> {
    (value: T): void;
}
export interface $Minifest {
}
export interface $ApkInfo {
    versionName: string;
    package: string;
    fileName: string;
}
