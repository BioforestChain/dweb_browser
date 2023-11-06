import { defineAPIOnFetch } from "../middleware-config.ts";
import { BaseRouter } from "./base-router.ts";

export class ApiRouter extends BaseRouter {
  constructor() {
    super();
  }

  use = defineAPIOnFetch;
  
}
