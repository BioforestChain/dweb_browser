package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.App.Companion.mainActivity
import info.bagen.rust.plaoc.webView.openDWebWindow

class MultiWebViewNMM : NativeMicroModule() {
    override val mmid: String = "mwebview.sys.dweb"

    private var viewTree: ViewTree = ViewTree()
    private val routers: Router = mutableMapOf()

    init {
        // 注册路由
        routers["/open"] = put@{ options ->
          val origin = if (options["origin"] == null) {
                 "Error not Found param origin"
            } else {
              options["origin"]!!
          }
            val processId = options["processId"]
            return@put openDwebView(origin,processId)
        }
        routers["/evalJavascript"] = put@{
            return@put true
        }
    }

    override fun bootstrap(routerTarget:String, options: NativeOptions): Any? {
        println("kotlin#MultiWebViewNMM bootstrap==> ${options["mainCode"]}  ${options["origin"]}")
        // 导航到自己的路由
        if (routers[routerTarget] == null) {
            return "mwebview.sys.dweb route not found for $routerTarget"
        }
        return routers[routerTarget]?.let { it->it(options) }
    }

    private fun openDwebView(origin: String,processId:String?): Any {
        println("Kotlin#MultiWebViewNMM openDwebView $origin")
        val webViewNode = viewTree.createNode(origin,processId)
        val append = viewTree.appendTo(webViewNode)
        // 当传递了父进程id，但是父进程是不存在的时候
        if(append == 0) {
            return "Error: not found mount process!!!"
        }
        // openDwebView
        if (mainActivity !== null) {
            openDWebWindow(activity = mainActivity!!.getContext(), url = origin)
        }
        return webViewNode.id
    }

    private fun closeDwebView(nodeId: Int): Boolean {
        return this.viewTree.removeNode(nodeId)
    }
}

class ViewTree {
    private val root = ViewTreeStruct(0, 0, "", mutableListOf())
    private var currentProcess = 0

    data class ViewTreeStruct(
        val id: Int,
        val processId: Int, //processId as parentId
        val origin: String,
        val children: MutableList<ViewTreeStruct?>
    )

    fun createNode(origin: String,processId:String?): ViewTreeStruct {
        // 当前要挂载到哪个父级节点
        var cProcessId = currentProcess
        //  当用户传递了processId，即明确需要挂载到某个view下
        if (processId !== null) {
            cProcessId = processId.toInt()
        }
        return ViewTreeStruct(
            id = cProcessId + 1,
            processId = cProcessId, // self add node id
            origin = origin,
            children = mutableListOf()
        )
    }

    fun appendTo(webViewNode: ViewTreeStruct): Int {
        val processId = webViewNode.processId
        fun next(node: ViewTreeStruct): Int {
            // 找到加入节点
            if (node.id == processId) {
                // 因为节点已经加入了，所以当前节点进程移动到新创建的节点
                currentProcess = webViewNode.id
                println("multiWebView#currentProcess:$currentProcess")
                node.children.add(webViewNode)
                return webViewNode.id
            }
            // 当节点还是小于当前父节点，就还需要BFS查找
            if (node.processId < processId) {
                for (n in node.children) {
                    return next(n as ViewTreeStruct)
                }
            }
            return 0
        }
        // 尾递归
        return next(this.root)
    }

    /**
     * 简单的移除节点
     */
    fun removeNode(nodeId: Int): Boolean {
        fun next(node: ViewTreeStruct): Boolean {
            for (n in node.children) {
                // 找到移除的节点
                if (n?.id == nodeId) {
                    return node.children.remove(n)
                }
            }
            // 当节点还是小于当前父节点，就还需要BFS查找
            if (node.processId < nodeId) {
                for (n in node.children) {
                    return next(n as ViewTreeStruct)
                }
            }
            return false
        }
        return next(this.root)
    }
}