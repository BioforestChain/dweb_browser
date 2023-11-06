import { defineWWWOnFetch } from "../middleware-config.ts";
import { BaseRouter } from "./base-router.ts";

export class WwwRouter extends BaseRouter {
  constructor() {
    super();
  }

  use = defineWWWOnFetch;
  
}
