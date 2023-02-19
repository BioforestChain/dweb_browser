package info.bagen.rust.plaoc.microService.sys.mwebview

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule

class MultiWebViewNMM : NativeMicroModule("mwebview.sys.dweb") {

    override suspend fun _bootstrap() {
        // 打开webview
        apiRouting
//        routers["/open"] = put@{ options ->
//            val origin = options["origin"] ?: return@put "Error not Found param origin"
//            val processId = options["processId"]
//            return@put openDwebView(origin, processId)
//        }
//        routers["/evalJavascript"] = put@{
//            return@put true
//        }
    }

    override suspend fun _shutdown() {
        apiRouting = null
    }

    private var viewTree: ViewTree = ViewTree()


    fun openDwebView(origin: String, processId: String?): Any {
        println("Kotlin#MultiWebViewNMM openDwebView $origin")
        /*val webViewNode = viewTree.createNode(origin,processId)
        val append = viewTree.appendTo(webViewNode)
        // 当传递了父进程id，但是父进程是不存在的时候
        if(append == 0) {
            return "Error: not found mount process!!!"
        }
        // openDwebView
        if (mainActivity !== null) {
            openDWebWindow(activity = mainActivity!!.getContext(), url = origin)
        }
        return webViewNode.id*/
        return App.mainActivity?.dWebBrowserModel?.openDWebBrowser(origin, processId)
            ?: "Error: not found mount process!!!"
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

    fun createNode(origin: String, processId: String?): ViewTreeStruct {
        // 当前要挂载到哪个父级节点
        var cProcessId = currentProcess
        //  当用户传递了processId，即明确需要挂载到某个view下
        if (!processId.isNullOrEmpty()) {
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