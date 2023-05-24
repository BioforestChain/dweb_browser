export declare const APIS: {
    createProcess: (env_script_url: string, metadata_json: string, env_json: string, fetch_port: MessagePort, name?: string) => Promise<{
        process_id: number;
    }>;
    runProcessMain: (process_id: number, config: $RunMainConfig) => void;
    createIpc: (process_id: number, mmid: string, ipc_port: MessagePort, env_json?: string) => Promise<void>;
    destroyProcess: (process_id: number) => void;
};
export type $RunMainConfig = {
    main_url: string;
};
