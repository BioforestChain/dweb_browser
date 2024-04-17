import { $IpcStreamAbort } from "./IpcStreamAbort";
import { $IpcStreamData } from "./IpcStreamData";
import { $IpcStreamEnd } from "./IpcStreamEnd";
import { $IpcStreamPaused } from "./IpcStreamPaused";
import { $IpcStreamPulling } from "./IpcStreamPulling";

export type $IpcStream = $IpcStreamData | $IpcStreamPulling | $IpcStreamPaused | $IpcStreamEnd | $IpcStreamAbort;
