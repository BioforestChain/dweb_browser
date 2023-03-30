export const routes = [
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
        matchMode: "full",
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
  
]


