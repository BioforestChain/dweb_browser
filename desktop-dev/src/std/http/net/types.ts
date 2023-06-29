export interface $DwebHttpServerOptions {
  port?: number;
  subdomain?: string;
}

export interface $ServerInfo<S> {
  server: S;

  host: string;
  hostname: string;
  port: number;
  origin: string;
  protocol: $Protocol;
}

export interface $Protocol {
  protocol: string;
  prefix: string;
  port: number;
}
