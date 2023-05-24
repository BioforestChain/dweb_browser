export declare const parseUrl: (url: string | URL, base?: string) => URL;
export declare const updateUrlOrigin: (url: string | URL, new_origin: string) => URL;
export declare const buildUrl: (url: URL, ext: {
    search?: string | URLSearchParams | Record<string, unknown> | {};
    pathname?: string;
}) => URL;
