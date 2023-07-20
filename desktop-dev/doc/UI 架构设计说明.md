## UI 架构设计说明

### 概述
- 整个应用由 2个 BrowserWindow 构成；
  - 其中一个用来承载全部的 app 内容的 BrowserWindow； [命名： bw_apps]; 等同于 pc电脑的桌面;
  - 其中一个用来承载全部的 app icon的 BrowserWindow; [命名： bw_icons]; 等同于 pc电脑的任务栏；

#### bw_apps 说明
  - 每一个 dweb-app 都用一个 BrowserView 来承载[命名： bv_app]
  - 全部的 bv_app 都集中在 bw_apps 中；
  - bv_app 都能够实现 `全屏` `缩小` `最小` `z-index堆叠` 这四个基础功能；
    - `全屏` - bv_app 填满整个 bw_apps;
    - `缩小` - bv_app 占据部分 bw_apps;
    - `最小` - bv_app 高度设置为 0， 在 bw_apps 不可见；
    - `z-index堆叠` - 设置 bv_app 在多个 bv_app 相对的位置，实现那个 bv_app 在前面显示；


#### bw_icons 说明
  - 用一个 browserView 承载全部 bv_app 对应的 icon;
  - 通过 icon 实现 bv_app 的激活 z-index堆叠 ... 功能；


