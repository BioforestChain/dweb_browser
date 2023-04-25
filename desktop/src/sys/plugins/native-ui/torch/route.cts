export const routes = [
    // {
    //     pathname: "/toast-ui/wait_for_operation",
    //     matchMode: "full",
    //     method: "GET"
    // },
    // {
    //     pathname: "/toast-ui/operation_return",
    //     matchMode: "full",
    //     method: "POST"
    // },
    // {
    //     pathname: "/toast.sys.dweb/startObserve",
    //     matchMode: "prefix",
    //     method: "GET"
    // },
    // {
    //     pathname: "/toast.sys.dweb/stopObserve",
    //     matchMode: "full",
    //     method: "GET"

    // },
    {
        pathname:"/torch.nativeui.sys.dweb/torchState",
        matchMode: "prefix",
        method: "GET"
    },
    {
        pathname:"/torch.nativeui.sys.dweb/toggleTorch",
        matchMode: "prefix",
        method: "GET"
    },
]


