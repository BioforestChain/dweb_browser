import type { $Core } from "../deps.ts";

export abstract class BaseRouter {
  abstract use(...onFetch: $Core.$OnFetch[]): void;
}
