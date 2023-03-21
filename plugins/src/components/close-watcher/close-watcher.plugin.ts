import { native_close_watcher_kit } from "./close-watcher.type.ts";

export class CloseWatcher{
    constructor(){
        const token = native_close_watcher_kit.create()
    }
}