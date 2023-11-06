import { defineEXTERNALOnFetch } from "../middleware-config.ts";
import { BaseRouter } from "./base-router.ts";

export class ExternalRouter extends BaseRouter {
  constructor() {
    super();
  }

  use = defineEXTERNALOnFetch;
}
