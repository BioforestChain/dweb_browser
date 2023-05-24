export declare function getAllApps(): Promise<unknown>;
export interface $AppMetaData {
    title: string;
    subtitle: string;
    id: string;
    downloadUrl: string;
    icon: string;
    images: string[];
    introduction: string;
    author: string[];
    version: string;
    keywords: string[];
    home: string;
    mainUrl: string;
    staticWebServers: $StaticWebServers[];
    openWebViewList: string[];
    size: string;
    fileHash: string;
    permissions: string;
    plugins: string[];
    releaseDate: string;
}
export interface $StaticWebServers {
    root: string;
    entry: string;
    subdomain: string;
    port: number;
}
/**
 * 第三方应用的 app信息
 */
export interface $AppInfo {
    folderName: string;
    appId: string;
    version: string;
    bfsAppId: string;
    name: string;
    icon: string;
}
