import type { $BaseRoute } from "../../base/base-add-routes-to-http.cjs"
export const routes: $BaseRoute[] = [
    {
        pathname: "/navigation-bar-ui/wait_for_operation",
        matchMode: "full",
        method: "GET"
    },
    {
        pathname: "/navigation-bar-ui/operation_return",
        matchMode: "full",
        method: "POST"
    },
    {
        pathname: "/navigation-bar.nativeui.sys.dweb/startObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname: "/navigation-bar.nativeui.sys.dweb/stopObserve",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/navigation-bar.nativeui.sys.dweb/getState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/navigation-bar.nativeui.sys.dweb/setState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        // /internal/observe?X-Dweb-Host=api.browser.sys.dweb%3A443&mmid=navigation-bar.nativeui.sys.dweb
        pathname:"/navigation-bar.nativeui.sys.dweb/internal/observe",
        matchMode: "full",
        method: "GET"
    }
]


