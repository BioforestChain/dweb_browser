//@ts-check
const http = require("node:http")
http.createServer((req, res) => {
    res.setHeader('Content-Type', 'text/html');
    res.write("<h1 id='xx'>asdasd</h1>") 
    setInterval(() => { 
        res.write(`<script>xx.innerHTML = "${Math.random()}"</script>`) 
    }, 1000)
}).listen(12002)


肇丰

## 这个 dwebServiceWorker 是否包括了两个部分
- 前端 [dwebServiceWorker-front] 
- 后台 [dwebServiceWorker-server]


## [dwebServiceWorker-front] 
- 通过 plugin 对象暴露出来 [plugin.dwebServiceWorker]
- plugin.dwebServiceWorker.update() 返回一个 Promise<UpdateController>
- UpdateController.pause() 暂停升级 
- UpdateController.remuse() ？取消暂停重新开始
- UpdateController.cancel() 取消升级
- UpdateController.addEventListener('start/progress/end/cancel')
- dwebServiceWorker.addEventListener("updatefound", () => { })
    - ？是否表示 plugin.dwebServiceWorker 每次启动的时候都会自动的查看是否有能够升级的 版本
        如果有 就执行 updateFound 这个事件；
    - ？ updateFound 这个事件是用户监听， 用户自己决定执行的逻辑

## [dwebServiceWorker-server]
- 实际上执行
    - app 下载开始的功能
    - app 下载暂停功能
    - app 下载取消暂停的功能
    - app 下载停止的功能
    - app 下载进度返回
    - app 查询是否有最新版本的功能

- ？
    - 需要写 desktop | android | ios 三个不同的版本
    - desktop 使用 js 
    - androi 使用 ktolin 写
    - ios 使用 swift 写


 

 


