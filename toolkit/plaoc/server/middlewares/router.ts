import type { $Core } from "../deps.ts";
import { BaseRouter } from "./base-router.ts";

export class Router extends BaseRouter {
  constructor() {
    super();
  }
  readonly handlers: $Core.$OnFetch[] = [];

  use(...handlers: $Core.$OnFetch[]) {
    this.handlers.push(...handlers);
  }
}
