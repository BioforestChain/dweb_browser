import { Ref, isRef } from "vue";
import LogPanel from "../components/LogPanel.vue";

const normalizeArgs = (args: any[]) => {
  const res: any[] = [];
  for (const arg of args) {
    if (isRef(arg)) {
      res.push(arg.value);
    } else {
      res.push(arg);
    }
  }
  return res;
};

export const defineLogAction = <T extends (...args: any[]) => Promise<unknown>>(
  fun: T,
  config: {
    logPanel: Ref<typeof LogPanel | undefined>;
    name: string;
    args: Array<unknown | Ref<unknown>>;
  }
) => {
  const nargs = () => normalizeArgs(config.args);
  return (async (...args: any[]) => {
    const logger = config.logPanel.value || console;
    logger.time(config.name, ...nargs());
    try {
      const result = await fun(...args);
      logger.timeEnd(
        config.name,
        ...nargs(),
        `<span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content shrink-0">-></span>`,
        result
      );
      return result;
    } catch (err) {
      logger.timeEnd(config.name, ...nargs());
      logger.error(config.name, err);
    }
  }) as unknown as T;
};
