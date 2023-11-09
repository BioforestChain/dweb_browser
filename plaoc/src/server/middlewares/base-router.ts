import type { $OnFetch } from "npm:@dweb-browser/js-process@0.1.4";

export abstract class BaseRouter {
  abstract use(...onFetch: $OnFetch[]): void;
}
