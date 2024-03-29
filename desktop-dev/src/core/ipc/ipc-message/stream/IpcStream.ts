import type { $IpcStreamAbort } from "./IpcStreamAbort";
import type { $IpcStreamData } from "./IpcStreamData";
import type { $IpcStreamEnd } from "./IpcStreamEnd";
import type { $IpcStreamPaused } from "./IpcStreamPaused";
import type { $IpcStreamPulling } from "./IpcStreamPulling";

export type $IpcStream = $IpcStreamData | $IpcStreamPulling | $IpcStreamPaused | $IpcStreamEnd | $IpcStreamAbort;
