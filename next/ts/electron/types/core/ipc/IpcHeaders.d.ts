export declare class IpcHeaders extends Headers {
    init(key: string, value: string): void;
    toJSON(): Record<string, string>;
}
