package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity

class BootNMM : NativeMicroModule() {
    override val mmid: Mmid = "boot.sys.dweb"
    private val hookBootApp = mutableMapOf<Mmid,AppRun>()
    private val registeredMmids = mutableSetOf<Mmid>()

    fun open(options: NativeOptions):Any {
        val origin = options["origin"]
        println("kotlinBootNMM start app:$origin,${hookBootApp.containsKey(origin)}")
        if (origin == null) {
            return "Error not Found param origin"
        }
        // 如果已经注册了该boot app
        if (hookBootApp.containsKey(origin)) {
          return (hookBootApp[origin]!!)(options) // 直接调用该方法
        }
        return "Error not register $origin boot Application"
    }

    private fun registerMicro(mmid: Mmid): Boolean {
        return registeredMmids.add(mmid)
    }

    private fun unRegisterMicro(mmid: Mmid): Boolean {
       return registeredMmids.remove(mmid)
    }

    private fun initBootApp() {
        hookBootApp["desktop.bfs.dweb"] = {
            openHomeActivity()
        }
    }
}
