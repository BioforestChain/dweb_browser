import type { $IpcStreamAbort } from "./IpcStreamAbort.ts";
import type { $IpcStreamData } from "./IpcStreamData.ts";
import type { $IpcStreamEnd } from "./IpcStreamEnd.ts";
import type { $IpcStreamPaused } from "./IpcStreamPaused.ts";
import type { $IpcStreamPulling } from "./IpcStreamPulling.ts";

export type $IpcStream = $IpcStreamData | $IpcStreamPulling | $IpcStreamPaused | $IpcStreamEnd | $IpcStreamAbort;
