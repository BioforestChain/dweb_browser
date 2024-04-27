import { pubTasks as toolkitNpmTasks } from "../toolkit/scripts/toolkit-pub.ts";
import { ConTasks } from "./helper/ConTasks.ts";

export const npmTasks = new ConTasks({}, import.meta.resolve("../"))
  ///
  .merge(toolkitNpmTasks, "toolkit");
