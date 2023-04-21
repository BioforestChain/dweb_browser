import type { $BaseRoute } from "../../base/base-add-routes-to-http.cjs"
export const routes :$BaseRoute[] = [
    {
        pathname: "/virtual-keyboard-ui/wait_for_operation",
        matchMode: "full",
        method: "GET"
    },
    {
        pathname: "/virtual-keyboard-ui/operation_return",
        matchMode: "full",
        method: "POST"
    },
    {
        pathname: "/virtual-keyboard.nativeui.sys.dweb/startObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname: "/virtual-keyboard.nativeui.sys.dweb/stopObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/virtual-keyboard.nativeui.sys.dweb/getState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/virtual-keyboard.nativeui.sys.dweb/setState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        // /internal/observe?X-Dweb-Host=api.browser.sys.dweb%3A443&mmid=virtual-keyboard.nativeui.sys.dweb
        pathname:"/virtual-keyboard.nativeui.sys.dweb/internal/observe",
        matchMode: "full",
        method: "GET"
    }
]


