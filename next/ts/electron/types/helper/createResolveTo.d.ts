/// <reference types="node" />
export declare const createResolveTo: (__dirname: string) => (...paths: string[]) => string;
export declare const ROOT: string;
export declare const resolveToRoot: (...paths: string[]) => string;
export declare const resolveToRootFile: (...paths: string[]) => import("url").URL;
