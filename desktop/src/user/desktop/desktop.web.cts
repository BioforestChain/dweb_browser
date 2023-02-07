/// <reference lib="DOM"/>
export const script = () => {
  const logEle = document.querySelector(
    "#readwrite-stream-log"
  ) as HTMLPreElement;
  const log = (...logs: any[]) => {
    logEle.append(document.createTextNode(logs.join(" ") + "\n"));
  };

  (
    document.querySelector("#test-readwrite-stream") as HTMLButtonElement
  ).onclick = async () => {
    const sid = Math.random().toString(36).slice(2);
    let acc = 0;
    const body = new ReadableStream({
      start(ctrl) {
        const ti = setInterval(() => {
          const msg = acc++;
          if (msg < 10) {
            ctrl.enqueue(new Uint32Array([msg]));
            log(sid, "request post:", msg);
          } else {
            ctrl.close();
            log(sid, "request end");
            clearInterval(ti);
          }
        }, 1000);
      },
    });
    const res = await fetch("./readwrite-stream", {
      method: "POST",
      body,
      duplex: "half",
    } as any);
    const res_reader = res.body!.getReader();
    while (true) {
      const item = await res_reader.read();
      if (item.done) {
        log(sid, "response done");
      } else {
        log(sid, "response got:", item.value);
      }
    }
  };
};
