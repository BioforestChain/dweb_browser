import { BaseRouter } from "./base-router.ts";
import { $OnFetch } from "./deps.ts";

export class Router extends BaseRouter {
  constructor() {
    super();
  }
  readonly handlers: $OnFetch[] = [];

  use(...handlers: $OnFetch[]) {
    this.handlers.push(...handlers);
  }
}
