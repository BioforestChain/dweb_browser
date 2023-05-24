/** 将 fetch 的参数进行标准化解析 */
export declare const normalizeFetchArgs: (url: RequestInfo | URL, init?: RequestInit) => {
    parsed_url: URL;
    request_init: RequestInit;
};
