import { Ref, isRef } from "vue";
import LogPanel from "../components/LogPanel.vue";

export const defineAction = <T extends (...args: any[]) => Promise<unknown>>(
  fun: T,
  config: {
    logPanel: Ref<typeof LogPanel | undefined>;
    name: string;
    args: Array<unknown | Ref<unknown>>;
  }
) => {
  const formatArgs = () => {
    const res: any[] = [];
    for (const arg of config.args) {
      if (isRef(arg)) {
        res.push(arg.value);
      } else {
        res.push(arg);
      }
    }
    return res;
  };
  return (async (...args: any[]) => {
    const logger = config.logPanel.value || console;
    logger.time(config.name, formatArgs());
    try {
      return await fun(...args);
    } catch (err) {
      logger.error(config.name, err);
    } finally {
      logger.timeEnd(config.name, formatArgs());
    }
  }) as unknown as T;
};
