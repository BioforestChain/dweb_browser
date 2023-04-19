import type { $BaseRoute } from "../base/base-add-routes-to-http.cjs"
export const routes :$BaseRoute[] = [
    {
        pathname: "/barcode-scanning-ui/wait_for_operation",
        matchMode: "full",
        method: "GET"
    },
    {
        pathname: "/barcode-scanning-ui/operation_return",
        matchMode: "full",
        method: "POST"
    },
    {
        pathname: "/barcode-scanning.nativeui.sys.dweb/startObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname: "/barcode-scanning.nativeui.sys.dweb/stopObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/barcode-scanning.nativeui.sys.dweb/getState",
        matchMode: "prefix",
        method: "GET"
    },
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
        // /internal/observe?X-Dweb-Host=api.browser.sys.dweb%3A443&mmid=barcode-scanning.nativeui.sys.dweb
        pathname:"/barcode-scanning.nativeui.sys.dweb/internal/observe",
        matchMode: "full",
        method: "GET"
    }
]


