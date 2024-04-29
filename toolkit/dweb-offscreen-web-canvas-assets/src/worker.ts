import { prepareInWorker } from "./prepare-in-worker.ts";
import { svgToBlob } from "./svg-rasterization.ts";
import { calcFitSize } from "./util/calcFitSize.ts";
// Move Three.js imports here if you use Three.js

const { toMainChannel } = prepareInWorker(import.meta.url);

(async () => {
  let wsUrl: string | null = null;
  let proxyUrl: string | null = null;
  const canvas = await new Promise<OffscreenCanvas>((resolve) =>
    toMainChannel.addEventListener(
      "message",
      (event) => {
        const { canvas, width, height, wsUrl: _wsUrl, proxyUrl: _proxyUrl } = event.data;
        canvas.width = width;
        canvas.height = height;
        resolve(canvas);
        wsUrl = _wsUrl || null;
        proxyUrl = _proxyUrl || null;
      },
      { once: true }
    )
  );

  const ctx = canvas.getContext("2d")!;

  const wrapUrlByProxy = (url: string) => {
    if (!proxyUrl) {
      return url;
    }
    // 如果是web专属的链接，那么直接在内部消化
    if (/^(data|blob):/.test(url)) {
      return url;
    }
    const proxyedUrl = new URL(proxyUrl);
    proxyedUrl.searchParams.set("url", url);
    return proxyedUrl.href;
  };

  const fetchImageBitmap = async (imageUrl: string, containerWidth: number, containerHeight: number) => {
    containerWidth = Math.min(containerWidth, 8192);
    containerHeight = Math.min(containerHeight, 8192);
    let imgSource: ImageBitmapSource;
    let needFitSize = true;
    if (typeof Image === "function") {
      const img = new Image();
      imgSource = img;
      img.src = imageUrl;
      await new Promise<Event>((resolve, reject) => {
        img.onload = resolve;
        img.onerror = reject;
      });
    } else {
      const res = await fetch(imageUrl);
      if (res.status !== 200) {
        throw new Error(`network error:${res.statusText}\n${await res.text()}`);
      }
      if (res.headers.get("Content-Type")?.includes("image/svg+xml")) {
        const svgCode = await res.text();
        imgSource = await svgToBlob(svgCode, {
          width: containerWidth,
          height: containerHeight,
        });
        needFitSize = false;
      } else {
        imgSource = await res.blob();
      }
    }

    let img = await createImageBitmap(imgSource);
    if (needFitSize) {
      const newSize = calcFitSize(img, {
        width: containerWidth,
        height: containerHeight,
      });
      if (img.width > newSize.width) {
        img = await createImageBitmap(img, {
          resizeWidth: newSize.width,
          resizeHeight: newSize.height,
        });
      }
    }
    return img;
  };
  const canvasToDataURL = async (canvas: OffscreenCanvas | HTMLCanvasElement, options?: ImageEncodeOptions) => {
    if ("toDataURL" in canvas) {
      return await canvas.toDataURL(options?.type, options?.quality);
    } else {
      return await blobToDataURL(await canvas.convertToBlob(options));
    }
  };
  const canvasToBlob = (canvas: OffscreenCanvas | HTMLCanvasElement, options?: ImageEncodeOptions) => {
    if ("toBlob" in canvas) {
      return new Promise<Blob>((resolve, reject) => {
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(blob);
            } else {
              reject("fail to blob");
            }
          },
          options?.type,
          options?.quality
        );
      });
    } else {
      return canvas.convertToBlob(options);
    }
  };
  const blobToDataURL = (blob: Blob) =>
    new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });

  const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor as FunctionConstructor;
  type $ReturnType = "void" | "json" | "string" | "binary";

  type $WsBinary = Blob | ArrayBufferLike | ArrayBufferView;
  const isWsBinary = (value: unknown): value is $WsBinary => {
    return value instanceof Blob || ArrayBuffer.isView(value) || value instanceof ArrayBuffer;
  };

  const evalCode = async (
    returnType: $ReturnType,
    code: string,
    cb: (error?: string, result?: string | $WsBinary) => void
  ) => {
    try {
      const res = (await new AsyncFunction(
        "canvas,ctx,fetchImageBitmap,blobToDataURL,canvasToDataURL,canvasToBlob,wrapUrlByProxy",
        code
      )(canvas, ctx, fetchImageBitmap, blobToDataURL, canvasToDataURL, canvasToBlob, wrapUrlByProxy)) as unknown;
      let result: string | $WsBinary | undefined;
      switch (returnType) {
        case "json":
          result = JSON.stringify(res);
          break;
        case "binary":
          if (isWsBinary(res)) {
            result = res;
          } else {
            throw new TypeError(`invalid binary type for result: ${result}`);
          }
          break;
        case "string":
          result = String(res);
          break;
        default: // void
      }
      cb(undefined, result);
    } catch (err) {
      console.error(code, err);
      cb(String(err), undefined);
    }
  };
  Object.assign(self, {
    evalCode,
    canvas,
    ctx,
    fetchImageBitmap,
    blobToDataURL,
    canvasToBlob,
    canvasToDataURL,
    wrapUrlByProxy,
  });

  if (wsUrl) {
    const ws = new WebSocket(wsUrl);

    interface $RunCommandReq {
      rid: number;
      returnType: $ReturnType;
      runCode: string;
    }

    ws.onopen = () => {
      console.info("ready for reander");
    };
    const execReq = (req: $RunCommandReq) => {
      console.log("start eval code:", req.rid, req.runCode);
      return evalCode(req.returnType, req.runCode, (error, success) => {
        try {
          if (error === undefined) {
            if (success !== undefined) {
              ws.send(`${req.rid}:return:`);
              ws.send(success);
            } else {
              ws.send(`${req.rid}:void`);
            }
          } else {
            ws.send(`${req.rid}:throw:${error}`);
          }
        } catch (err) {
          ws.send(`${req.rid}:throw:${String(err)}`);
        }
      });
    };
    let queue: Promise<void> | undefined;
    const queueReq = (req: $RunCommandReq) => {
      if (queue) {
        queue = queue.finally(() => execReq(req));
      } else {
        queue = execReq(req);
      }
    };
    ws.onmessage = (ev) => {
      const req = JSON.parse(ev.data) as $RunCommandReq;
      queueReq(req);
    };
  } else {
    console.log("start tests");
    const testResize = () => {
      canvas.width = 300;
      canvas.height = 300;
      return {
        width: canvas.width,
        height: canvas.height,
      };
    };
    const testDrawImage = async () => {
      const img = await fetchImageBitmap(
        "https://upload.wikimedia.org/wikipedia/commons/5/55/John_William_Waterhouse_A_Mermaid.jpg",
        100,
        100
      );
      ctx.drawImage(img, 0, 0);

      return await canvasToDataURL(canvas);
    };
    for (const [test, config] of new Map<Function, $ReturnType>([
      [testResize, "json"],
      [testDrawImage, "string"],
    ])) {
      await evalCode(config, test.toString().match(/\{([\w\W]+)\}/)![1], (err, result) =>
        err ? console.error(test.name, err) : console.log(test.name, result)
      );
    }
  }
})().catch(console.error);