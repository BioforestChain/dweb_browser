//@ts-check
const ws = new WebSocket(new URL("/channel", location.href.replace("http", "ws")));
/**
 * @type {<T>()=>{promise:Promise<T>,resolve:(value:T)=>void,reject:(err:any?)=>void}}
 */
const promiseWithResolvers = () => {
  let resolve;
  let reject;
  const promise = new Promise((_resolve, _reject) => {
    resolve = _resolve;
    reject = _reject;
  });
  return { promise, resolve, reject };
};

const peerRs = promiseWithResolvers();
const reqResChannelRs = promiseWithResolvers();
const superChannelRs = promiseWithResolvers();
const encoder = new TextEncoder();
const decoder = new TextDecoder();
/**
 * @type {RTCDataChannel?}
 */
let channel = null;
const totalSize = (1024 * 1024 * 1024) / 100;
const unitSize = 1024 * 64;
ws.addEventListener("message", async (event) => {
  const data = event.data;
  if (data === "🔒") {
    ws.send("🔓");
  } else if (data === "start-post") {
    for (let sendSize = 0; sendSize < totalSize; sendSize += unitSize) {
      ws.send(new Uint8Array(unitSize));
    }
    console.log("end post");
    ws.send("end-post");
  } else if (data instanceof Blob) {
    // console.log(data.size);
  } else if (typeof data === "string") {
    if (data.startsWith("data-channel:")) {
      const peer = new RTCPeerConnection();
      peerRs.resolve(peer);
      await peer.setRemoteDescription(JSON.parse(data.substring("data-channel:".length)));
      peer.ondatachannel = (e) => {
        channel = e.channel;
        channel.bufferedAmountLowThreshold = Math.ceil(unitSize / 2);
        console.log("open-channel", channel);
        channel.onmessage = (e) => {
          const { data: arraybuffer } = e;
          if (arraybuffer instanceof ArrayBuffer) {
            if (arraybuffer.byteLength < 1024) {
              const data = decoder.decode(arraybuffer);
              if (data.startsWith("echo:")) {
                channel?.send(arraybuffer);
              }
            }
          }
        };
        ws.send("open-channel");
      };
      peer.onicecandidate = (e) => {
        const candidate = e.candidate;
        if (candidate) {
          ws.send(`icecandidate:${JSON.stringify(candidate)}`);
        }
      };

      const answer = await peer.createAnswer();
      peer.setLocalDescription(answer);
      ws.send(`remote:${JSON.stringify(answer)}`);
    } else if (data.startsWith("icecandidate:")) {
      const peer = await peerRs.promise;
      const candidate = JSON.parse(data.substring("icecandidate:".length));
      peer.addIceCandidate(candidate);
    } else if (data === "start-channel-post" && channel !== null) {
      let i = 1;
      for (let sendSize = 0; sendSize < totalSize; sendSize += unitSize) {
        channel.send(new Uint8Array(unitSize));
        console.log("channel.bufferedAmount", channel.bufferedAmount);
        if (channel.bufferedAmount > channel.bufferedAmountLowThreshold) {
          const { promise, resolve } = promiseWithResolvers();
          channel.onbufferedamountlow = resolve;
          await promise;
        }
        const cur = i++;
        if (cur === 44) {
          await new Promise((cb) => setTimeout(cb, 1000));
        }
      }
      channel.send(encoder.encode("end-channel-post"));
      console.log("end channel post");
    } else if (data === "start-req-res-channel") {
      /**
       * @type {ReadableStream<Uint8Array>}
       */
      const output = new ReadableStream({
        async start(outputController) {
          const input = (await fetch("/s2c-channel")).body?.getReader();
          if (input === undefined) {
            console.error("no support get reader from http-get-body");
            return;
          }
          const size32 = new Uint32Array(1);
          const size8 = new Uint8Array(size32.buffer);
          const reqResChannel = {
            on: new EventTarget(),
            /**
             * @param {Uint8Array|string} data
             */
            send(data) {
              if (typeof data === "string") {
                data = encoder.encode(data);
              }
              if (data.byteLength === 0) {
                return;
              }
              size32[0] = data.byteLength;
              outputController.enqueue(size8);
              outputController.enqueue(data);
            },
            /**
             * @param {any?} reason
             */
            close(reason = undefined) {
              input?.cancel(reason);
              if (reason) {
                outputController.error(reason);
              } else {
                outputController.close();
              }
            },
          };
          reqResChannelRs.resolve(reqResChannel);
          (async () => {
            let cache = new Uint8Array(0);
            /**
             * @param {Uint8Array} a1
             * @param {Uint8Array} a2
             */
            const contact = (a1, a2) => {
              const a3 = new Uint8Array(a1.byteLength + a2.byteLength);
              a3.set(a1);
              a3.set(a2, a1.byteLength);
              return a3;
            };
            let waitSize = 0;
            while (true) {
              const item = await input.read();
              if (item.done) {
                break;
              }
              cache = cache.byteLength === 0 ? item.value : contact(cache, item.value);
              if (waitSize === 0) {
                if (cache.length < 4) {
                  continue;
                }
                const sizeinfo = cache.subarray(0, 4);
                cache = cache.subarray(4);
                size8.set(sizeinfo);
                waitSize = size32[0];
              } else {
                if (cache.byteLength < waitSize) {
                  continue;
                }
                const message = cache.subarray(0, waitSize);
                cache = cache.subarray(waitSize);
                reqResChannel.on.dispatchEvent(new MessageEvent("message", { data: message }));
              }
            }
          })();
          reqResChannel.on.addEventListener(
            "message",
            /**
             * @param {MessageEvent<Uint8Array>} e
             */
            (e) => {
              const data = e.data;
              if (data.byteLength < 1024) {
                const msg = decoder.decode(data);
                if (msg.startsWith("echo:")) {
                  reqResChannel.send(msg);
                }
              }
            }
          );
        },
      });
      void fetch("/c2s-channel", {
        method: "POST",
        body: output,
        duplex: "half",
      }).catch(console.error);
    } else if (data == "start-req-res-post") {
      const reqResChannel = await reqResChannelRs.promise;
      for (let sendSize = 0; sendSize < totalSize; sendSize += unitSize) {
        reqResChannel.send(new Uint8Array(unitSize));
      }
      console.log("end req res post");
      reqResChannel.send("end-req-res-post");
    } else if (data === "start-super-channel") {
      let it = 0;
      /**
       * @type {Array<WebSocket>}
       */
      const wss = [];
      const superChannel = {
        on: new EventTarget(),
        /**
         * @type {(ws:WebSocket)=>void}
         */
        addSource(ws) {
          wss.push(ws);
          ws.onmessage = (event) => {
            if (event.data === "🔒") {
              //   ws.postMessage("🔓");
              ws.send("🔓");
              return;
            }
            superChannel.on.dispatchEvent(new MessageEvent("message", { data: event.data }));
          };
        },
        /**
         * @type {(msg:string | ArrayBuffer)=>void}
         */
        send(msg) {
          const ws = wss[it++ % wss.length];
          if (!ws) {
            throw new Error();
          }
          //   if (msg instanceof ArrayBuffer) {
          //     ws.postMessage(msg, [msg]);
          //   } else {
          //     ws.postMessage(msg);
          //   }
          ws.send(msg);
        },
      };
      superChannelRs.resolve(superChannel);
      for (let i = 0; i < 2; i++) {
        superChannel.addSource(new WebSocket(new URL("/super-channel", location.href.replace("http", "ws"))));
        // superChannel.addSource(new Worker("/worker.js"));
      }
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      //   superChannel.addSource(new Worker("/worker.js"));
      superChannel.on.addEventListener(
        "message",
        /**
         * @param {MessageEvent<Blob|string>} e
         */
        (e) => {
          const data = e.data;
          if (typeof data === "string" && data.startsWith("echo:")) {
            superChannel.send(data);
          }
        }
      );
    } else if (data === "start-super-post") {
      const superChannel = await superChannelRs.promise;
      for (let sendSize = 0; sendSize < totalSize; sendSize += unitSize) {
        superChannel.send(new Uint8Array(unitSize));
      }
      console.log("end super post");
      superChannel.send("end-super-post");
    }
  }
});

// 0.333mb: native2js:[41.626762172s] 24.60001mb/s
// 0.5mb: native2js:[21.250698273s] 48.188236mb/s
// 0.666mb: native2js:[19.267420722s] 53.14787mb/s
// 1mb: native2js:[16.476599837s] 62.15101mb/s
// 1.1mb: native2js:[16.772527806s] 61.054142mb/s | native2js:[16.721236712s] 61.240353mb/s
// 1.2mb: native2js:[17.103133223s] 59.872536mb/s

// 0.1mb:  | js2native:[1m 42.949282773s] 9.946672mb/s
// 1mb: js2native:[1m 51.147297927s] 9.213024mb/s | js2native:[1m 42.949282773s] 9.946672mb/s
// 10mb:  | js2native:[1m 39.062641889s] 10.39753mb/s
