import { debounce } from "@dweb-browser/helper/decorator/$debounce.ts";
import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import { normalizeFilePath } from "./WalkDir.ts";

export async function* watchFs(
  paths: string | string[],
  options: {
    ignore?: (path: string) => boolean;
    recursive?: boolean;
    debounceMs?: number;
  } = {}
) {
  const { recursive = true, debounceMs = 100, ignore = () => false } = options;
  console.log("watch", paths);
  const watcher = Deno.watchFs(paths, { recursive });
  let _controller!: ReadableStreamDefaultController;
  const changes = new Map<Deno.FsEvent["kind"], { paths: Set<string>; consume: ReturnType<typeof debounce> }>();
  const changeStream = new ReadableStream<{ kind: Deno.FsEvent["kind"]; paths: string[] }>({
    async start(controller) {
      _controller = controller;
      for await (const event of watcher) {
        const change = mapHelper.getOrPut(changes, event.kind, () => {
          const change = {
            paths: new Set(event.paths),
            consume: debounce(() => {
              const pathList = Array.from(change.paths, (path) => normalizeFilePath(path));
              if (false === pathList.every(ignore)) {
                console.log("file", event.kind, ...pathList);
                changes.delete(event.kind);
                controller.enqueue({ kind: event.kind, paths: pathList });
              }
            }, debounceMs),
          };
          return change;
        });

        event.paths.forEach((path) => change.paths.add(path));
        change.consume();
      }
    },
  });
  const reader = changeStream.getReader();
  try {
    while (true) {
      const item = await reader.read();
      if (item.done) {
        return;
      }
      yield item.value;
    }
  } catch (e) {
    _controller.error(e);
  } finally {
    watcher.close();
    _controller.close();
    for (const change of changes.values()) {
      change.consume.reset();
    }
    changes.clear();
  }
}
