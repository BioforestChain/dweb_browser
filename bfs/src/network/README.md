## 分布式网络

bfs://192.168.0.1:1234/

## 安全

### 传输非对称加密

保证传输安全，需要先向接收方索取公钥加密消息，然后发送给接收方解密。

### 数字签名防止篡改

大文件： 单项散列函数生成128位摘要，使用私钥加密128位摘要.
然后发送的数据包为文件+加密的摘要+公钥。

## 地址管理

每个节点在运行的时候本地至少能路由到两个节点.可以设置黑名单，白名单。

### 动态加载

动态的地址表加载,动态的地址更新，在每个节点更新自己的节点地址的时候，向所有的节点发送一个事件。

## 发布订阅

每个节点可以订阅别的节点的事件（最基础的：关闭，端口/ip修改，开启，对称秘钥更新）

节点之间可以订阅一些钩子事件，也可以拒绝别人的事件。

## ping命令

节点间通过查看是否存活。

## 节点查找

图遍历，需要防止网络风暴。
