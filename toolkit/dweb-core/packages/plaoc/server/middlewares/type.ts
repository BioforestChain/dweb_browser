
export interface $Route {
  get(path: string, handler: RequestHandler): void;
  post(path: string, handler: RequestHandler): void;
  delete(path: string, handler: RequestHandler): void;
  put(path: string, handler: RequestHandler): void;
}

export type RequestHandler = (ctx: $Context) => void;

export interface $Context {
  request: Request;
  text(response: string): void;
  json(response: string): void;
  empty(): void;
  number(response: number): void;
  bit(response: Uint8Array): void;
  stream(response: ReadableStream): void;
}

export type METHOD = string