# 目录说明
- plugins 目录为第三方app注入模拟访问移动端系统给的功能
- 本质上是 WebComponent 组件


## 访问的标准方式
- 组件内部的标准向外发送请求的标准访问方式是 fetch("./path")
- 通过所注入的 .html 匹配的 .worker 服务器向外发送数据通信;

## 注入的方法
- 通过向 / || /index.html 的请求中 respoonse.body 直接加入返回


## 调用方法返回的数据必须是 
- Promise<boolean | string | number>

 