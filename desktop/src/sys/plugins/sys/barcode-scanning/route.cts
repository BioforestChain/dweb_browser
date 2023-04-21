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
    {
        pathname: "/barcode-scanning-ui/wait_for_operation",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname: "/barcode-scanning-ui/operation_return",
        matchMode: "prefix",
        method: "POST"
    },
    {
        pathname: "/camera.sys.dweb/getPhoto",
        matchMode: "prefix",
        method: "GET"
    }

]


