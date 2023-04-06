// 数据准备工作
function createPropertyDescriptor(selector){
    let el = undefined;
    let o = {
        get(){
            if(window.parentElement === undefined) throw new Error(`window.parentElement === undefined`)
            if(el !== undefined) return el
            el = window.parentElement.querySelector(selector)
            console.log(`触发了get`, o)
            return el;
        }
    }  
    return o
}

const plugins = [
    {key: "statusBar", selector: "#statusbar"},
    {key: "navigationBar", selector: "#navgation-bar"},
    {key: "safeArea", selector: "#safe-area"},
    {key: "virtualKeyboard", selector: "#virtual-keyboard"}
]

function definePlugins(){
    plugins.forEach(({key, selector}) => {
        Object.defineProperty(
            window, 
            key, 
            createPropertyDescriptor(selector)
        )
    })
}

definePlugins()