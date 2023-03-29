export const routes = [
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
    // {
    //     pathname: "/operation_return",
    //     matchMode: "full",
    //     method: "GET"
    // },
    {
        pathname: "/navigation-bar.nativeui.sys.dweb/startObserve",
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
    // {
    //     // /internal/observe?X-Dweb-Host=api.browser.sys.dweb%3A443&mmid=status-bar.nativeui.sys.dweb
    //     pathname:"/internal/observe",
    //     matchMode: "full",
    //     method: "GET"
    // }
]


