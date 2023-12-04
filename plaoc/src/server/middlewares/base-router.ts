import type { $OnFetch } from "npm:@dweb-browser/js-process@0.1.6";

export abstract class BaseRouter {
  abstract use(...onFetch: $OnFetch[]): void;
}
