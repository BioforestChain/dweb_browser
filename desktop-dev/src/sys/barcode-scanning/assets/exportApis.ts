import { importApis } from "../../../helper/openNativeWindow.preload.ts";
import type { BarcodeScanningNMM } from "../barcode-scanning.main.ts";
const mainApis = importApis<BarcodeScanningNMM["_exports"]>();

// 提供一个基于comlink的双工通讯
if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis } = await import(
      "../../../helper/openNativeWindow.preload.ts"
    );
    exportApis(globalThis);
  })();
}

// class BarcodeDetectorDelegate {
//   barcodeDetector = new BarcodeDetector({
//     formats: ["qr_code", "code_39", "codabar", "ean_13"],
//   });

//   async process(blob: Blob): Promise<string[]> {
//     const po = new PromiseOut<string[]>();
//     const img = new Image();
//     img.src = URL.createObjectURL(blob);
//     img.onload = () => {
//       // 在图像加载完成后执行以下步骤
//       const canvas = document.createElement("canvas");
//       const ctx = canvas.getContext("2d")!;
//       canvas.width = img.width;
//       canvas.height = img.height;
//       ctx.drawImage(img, 0, 0);
//       const imageData = ctx.getImageData(0, 0, img.width, img.height);
//       this.barcodeDetector
//         .detect(imageData)
//         .then((barcodes) => {
//           const data = barcodes.map((barcode) => {
//             return barcode.rawValue;
//           });
//           po.resolve(data);
//           return barcodes;
//         })
//         .catch((err: Error) => {
//           console.log(err);
//           po.reject(err);
//         });
//     };
//     return await po.promise;
//   }

//   getSupportedFormats(): string[] {
//     throw new Error("Method not implemented.");
//   }

//   static getSupportedFormats(): string[] {
//     return BarcodeDetector.getSupportedFormats();
//   }
// }

const formats = ["qr_code", "code_39", "codabar", "ean_13"];
const barcodeDetector = new BarcodeDetector({
  formats: formats,
});

let processResolveId: number | undefined;
let stopProcessResolveId: number | undefined;
// 解析
async function process(u8a: Uint8Array, resolveId: number) {
  processResolveId = resolveId;
  const blob = new Blob([u8a], { type: "image/jpeg" });
  const img = new Image();
  img.src = URL.createObjectURL(blob);
  img.onload = () => {
    // 检查是否需要停止解析
    if (resolveId === stopProcessResolveId) {
      mainApis.operationCallback([], resolveId);
      processResolveId = undefined;
      stopProcessResolveId = undefined;
    }
    // 在图像加载完成后执行以下步骤
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d")!;
    canvas.width = img.width;
    canvas.height = img.height;
    ctx.drawImage(img, 0, 0);
    const imageData = ctx.getImageData(0, 0, img.width, img.height);
    barcodeDetector
      .detect(imageData)
      .then((barcodes) => {
        // 检查是否需要停止解析
        if (resolveId === stopProcessResolveId) {
          mainApis.operationCallback([], resolveId);
        } else {
          const data = barcodes.map((barcode) => {
            return barcode.rawValue;
          });
          console.log("data: ", data);
          mainApis.operationCallback(data, resolveId);
        }
        processResolveId = undefined;
        stopProcessResolveId = undefined;
      })
      .catch((err: Error) => {
        mainApis.operationCallback(err, resolveId);
      });
  };
}

// 停止正在解析的二维码的操作；
async function stop(resolveId: number) {
  if (processResolveId === undefined) {
    // 当前没有正在解析的操作
    return mainApis.operationCallback(true, resolveId);
  }
  stopProcessResolveId = processResolveId;
  mainApis.operationCallback(true, resolveId);
}

async function getSupportedFormats(resolveId: number) {
  mainApis.operationCallback(formats, resolveId);
}

export const APIS = {
  process,
  stop,
  getSupportedFormats,
};

Object.assign(globalThis, APIS);
if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis } = await import(
      "../../../helper/openNativeWindow.preload.ts"
    );
    exportApis(globalThis);
  })();
}
