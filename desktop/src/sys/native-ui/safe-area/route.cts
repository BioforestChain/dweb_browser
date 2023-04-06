import type { $BaseRoute } from "../base/base-add-routes-to-http.cjs"
export const routes: $BaseRoute[]  = [
    {
        pathname: "/safe-area-ui/wait_for_operation",
        matchMode: "full",
        method: "GET"
    },
    {
        pathname: "/safe-area-ui/operation_return",
        matchMode: "full",
        method: "POST"
    },
    {
        pathname: "/safe-area.nativeui.sys.dweb/startObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname: "/safe-area.nativeui.sys.dweb/stopObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/safe-area.nativeui.sys.dweb/getState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/safe-area.nativeui.sys.dweb/setState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/safe-area.nativeui.sys.dweb/internal/observe",
        matchMode: "full",
        method: "GET"
    }
]


