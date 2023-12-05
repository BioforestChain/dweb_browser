import type { $OnFetch } from "./deps.ts";

export abstract class BaseRouter {
  abstract use(...onFetch: $OnFetch[]): void;
}
