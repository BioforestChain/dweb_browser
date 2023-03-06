import { registerWebPlugin } from "../../registerPlugin.cjs"
import { BarcodeScanner } from "./barcodeScanner.plugin.cjs"


registerWebPlugin(new BarcodeScanner())
