import type { $BaseRoute } from "../../base/base-add-routes-to-http.cjs"
export const routes :$BaseRoute[] = [
    {
        // 扫一扫进程
        pathname:"/barcode-scanning.sys.dweb/process",
        matchMode: "prefix",
        method: "OPTIONS"
    },
    {
        // 扫一扫进程
        pathname:"/barcode-scanning.sys.dweb/process",
        matchMode: "prefix",
        method: "POST"
    },
]


