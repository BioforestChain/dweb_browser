import { $OnFetch } from "npm:@dweb-browser/js-process@0.1.4";
import { BaseRouter } from "./base-router.ts";

export class Router extends BaseRouter {
  constructor() {
    super();
  }
  readonly handlers: $OnFetch[] = [];

  use(...handlers: $OnFetch[]) {
    this.handlers.push(...handlers);
  }
}
