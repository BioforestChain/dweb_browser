# android 没有进入 demo 页面的情况
- 进入 /Users/pengxiaohua/project/dweb_browser/android/app/src/main/java/info/bagen/rust/plaoc/microService/main.kt
- 更改为

```
 val bootMmidList = when (DEVELOPER.CURRENT) {
        DEVELOPER.GAUBEE -> listOf(
//            cotJMM.mmid,
//            cotDemoJMM.mmid,
            browserNMM.mmid,
        )
        DEVELOPER.HuangLin, DEVELOPER.HLVirtual -> listOf(browserNMM.mmid)
        DEVELOPER.WaterBang -> listOf(cotDemoJMM.mmid,browserNMM.mmid)
        else -> {
            listOf(
                cotDemoJMM.mmid, // 需要增加这个 mmid 即可
                browserNMM.mmid,
            )
        }
    }

```