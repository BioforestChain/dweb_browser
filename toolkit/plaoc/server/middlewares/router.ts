import type { $OnFetch } from "../deps.ts";
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
