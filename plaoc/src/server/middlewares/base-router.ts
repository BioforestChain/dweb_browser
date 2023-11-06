import type { $OnFetch } from "npm:@dweb-browser/js-process";

export abstract class BaseRouter {
  abstract use(...onFetch: $OnFetch[]): void;
}
