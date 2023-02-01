"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.APIS = void 0;
const ALL_WORKER_MAP = new Map();
let acc_worker_id = 0;
const createWorker = (main_code) => {
    const worker_id = acc_worker_id++;
    const worker = new Worker(`data:utf-8,${main_code}`);
    ALL_WORKER_MAP.set(worker_id, worker);
    /// TODO 使用 weaklock 来检测线程是否唤醒
    return worker_id;
};
const createIpc = (worker_id) => {
    const worker = ALL_WORKER_MAP.get(worker_id);
    if (worker === undefined) {
        throw new Error(`no found worker by id: ${worker_id}`);
    }
    const channel = new MessageChannel();
    worker.postMessage(channel.port2, [channel.port2]);
    return channel.port1;
};
exports.APIS = {
    createWorker,
    createIpc,
};
