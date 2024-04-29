import { ConTasks } from "../../scripts/helper/ConTasks.ts";

const plaocTasks = new ConTasks(
  {
    "plaoc:server:dist": {
      cmd: "deno",
      args: "task bundle:watch:server"
    },
    "plaoc:client": {
      cmd: "deno",
      args: "task build:client:watch"
    }
  },
  import.meta.resolve("../../")
)

if(import.meta.main) {
  plaocTasks.spawn([...Deno.args, "--dev"]);
}