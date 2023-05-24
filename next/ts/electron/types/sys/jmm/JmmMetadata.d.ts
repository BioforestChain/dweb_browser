import { $MMID } from "../../helper/types.js";
export declare class JmmMetadata {
    readonly config: $JmmMetadata;
    constructor(config: $JmmMetadata);
}
export interface $JmmMetadata {
    id: $MMID;
    server: $JmmMetadata.$MainServer;
}
export declare namespace $JmmMetadata {
    interface $MainServer {
        root: string;
        entry: string;
    }
}
