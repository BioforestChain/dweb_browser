/**
 * 将字符串解码成 jsonlines 格式
 */
export class JsonlinesStream<T> extends TransformStream<string, T> {
  constructor() {
    let json = "";

    const try_enqueue = (
      controller: TransformStreamDefaultController<T>,
      jsonline: string
    ) => {
      try {
        controller.enqueue(JSON.parse(jsonline));
      } catch (err) {
        controller.error(err);
        return true;
      }
    };
    super({
      transform: (chunk, controller) => {
        json += chunk;
        let line_break_index: number;
        while ((line_break_index = json.indexOf("\n")) !== -1) {
          const jsonline = json.slice(0, line_break_index);
          json = json.slice(jsonline.length + 1);

          if (try_enqueue(controller, jsonline)) {
            break;
          }
        }
      },
      flush: (controller) => {
        json = json.trim();
        if (json.length > 0) {
          try_enqueue(controller, json);
        }
        controller.terminate();
      },
    });
  }
}
