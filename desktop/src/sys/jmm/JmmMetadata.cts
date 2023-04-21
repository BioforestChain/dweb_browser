export class JmmMetadata {
  constructor(readonly config: $JmmMetadata) {}
}
export interface $JmmMetadata {
  id: $MMID;
  server: $JmmMetadata.$MainServer;
}

export namespace $JmmMetadata {
  export interface $MainServer {
    root: string;
    entry: string;
  }
}
