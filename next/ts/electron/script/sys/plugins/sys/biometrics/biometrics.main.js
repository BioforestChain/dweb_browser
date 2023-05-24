"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BiometricsNMM = void 0;
const micro_module_native_js_1 = require("../../../../core/micro-module.native.js");
const devtools_js_1 = require("../../../../helper/devtools.js");
class BiometricsNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "biometrics.sys.dweb"
        });
        Object.defineProperty(this, "allocId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "httpNMM", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "impactLightStyle", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "HEAVY"
        });
        Object.defineProperty(this, "notificationStyle", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "SUCCESS"
        });
        Object.defineProperty(this, "duration", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "waitForOperationResMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "encoder", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new TextEncoder()
        });
        Object.defineProperty(this, "reqResMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        // private _check = async (req: IncomingMessage, res: OutgoingMessage) => {
        //   const origin = req.headers.origin;
        //   if(origin === undefined) throw new Error(`${this.mmid} origin === undefined`);
        //   const waitForRes = this.waitForOperationResMap.get(origin);
        //   if(waitForRes === undefined) {
        //     console.log('origin: ', origin)
        //     console.log('this.waitForOperationResMap: ', this.waitForOperationResMap)
        //     throw new Error(`${this.mmid} waitForRes === undefined`)
        //   };
        //   const id = this.allocId++;
        //   const value = this.encoder.encode(
        //     `${JSON.stringify({
        //       operationName: "check",
        //       value: "",
        //       from: origin,
        //       id: id 
        //     })}\n`
        //   )
        //   waitForRes.write(value)
        //   this.reqResMap.set(id, {req, res});
        // }
        // private _biometrics = async (req: IncomingMessage, res: OutgoingMessage) => {
        //   const origin = req.headers.origin;
        //   if(origin === undefined) throw new Error(`origin === undefined`);
        //   const waitForRes = this.waitForOperationResMap.get(origin);
        //   if(waitForRes === undefined) throw new Error(`waitForRes === undefined`)
        //   const id = this.allocId++;
        //   const value = this.encoder.encode(`
        //     ${JSON.stringify({
        //       operationName: "biometrics",
        //       value: '',
        //       from: origin,
        //       id: id
        //     })}
        //   `)
        //   waitForRes.write(value)
        //   this.reqResMap.set(id, {req, res});
        // }
        // private _waitForOperation = async (req: IncomingMessage, res: OutgoingMessage) => {
        //   const queyString = req.url?.split("?")[1]
        //   if(queyString === undefined) throw new Error(`${this.mmid} search === undefined`);
        //   const parsedUrlQuery = querystring.parse(queyString);
        //   const appUrl = parsedUrlQuery.app_url;
        //   if(appUrl === undefined) throw new Error(`${this.mmid} appUrl === undefined`);
        //   if(typeof appUrl === "string"){
        //     return this.waitForOperationResMap.set(appUrl, res);
        //   }
        //   throw new Error(`${this.mmid} appUrl === Array`)
        // }
        // private _operationReturn = async (req: IncomingMessage, res: OutgoingMessage) => {
        //   let chunks = Buffer.alloc(0);
        //   req.on('data', (chunk) => {
        //     chunks = Buffer.concat([chunks, chunk]);
        //   })
        //   req.on('end', () => {
        //     res.end();
        //     const {err, id} = this.getIdFromReq(req);
        //     if(err) throw err;
        //     const key = parseInt(id)
        //     const reqRes = this.reqResMap.get(key);
        //     if(reqRes === undefined){
        //       console.log('id', id)
        //       console.log("this.reqResMap: ", this.reqResMap)
        //       throw new Error(`reqRes === undefined`)
        //     };
        //     reqRes.res.end(chunks);
        //     this.reqResMap.delete(key)
        //   })
        // }
        Object.defineProperty(this, "getIdFromReq", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (req) => {
                const id = req.headers.id;
                if (id === undefined) {
                    return {
                        err: new Error(`id === undefined`),
                        id: "",
                    };
                }
                if (typeof id === "string") {
                    return {
                        err: null,
                        id: id
                    };
                }
                return {
                    err: new Error(`id === Array`),
                    id: ""
                };
            }
        });
    }
    async _bootstrap(context) {
        devtools_js_1.log.green(`[${this.mmid}] _bootstrap`);
        this.registerCommonIpcOnMessageHandler({
            pathname: "/check",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async () => {
                return true;
            }
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/biometrics",
            matchMode: "full",
            input: {},
            output: "object",
            handler: async () => {
                return await this.nativeFetch(`file://mwebview.sys.dweb/plubin/biommetrices`);
            }
        });
    }
    _shutdown() {
        throw new Error("Method not implemented.");
    }
}
exports.BiometricsNMM = BiometricsNMM;
