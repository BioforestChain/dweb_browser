export const routes = [
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
        pathname:"/safe-area.nativeui.sys.dweb/getState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/safe-area.nativeui.sys.dweb/setState",
        matchMode: "prefix",
        method: "GET"
    },
    // {
    //     // /internal/observe?X-Dweb-Host=api.browser.sys.dweb%3A443&mmid=status-bar.nativeui.sys.dweb
    //     pathname:"/internal/observe",
    //     matchMode: "full",
    //     method: "GET"
    // }
]


