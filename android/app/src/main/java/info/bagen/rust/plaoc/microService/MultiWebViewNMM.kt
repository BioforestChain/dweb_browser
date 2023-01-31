package info.bagen.rust.plaoc.microService

typealias code = String

interface WindowOptions {
    val processId: Int?  // 要挂载的父进程id
}


class MultiWebViewNMM : NativeMicroModule() {
    override val mmid = "mwebview.sys.dweb"
    private var viewTree: ViewTree = ViewTree()
    private val routers: Router = mutableMapOf()

    init {
        // 注册路由
        routers["/open"] = put@{
            return@put openDwebView(it as WindowOptions)
        }
        routers["/evalJavascript/(:webview_id)"] = put@{
            return@put true
        }
    }


    private fun openDwebView(args: WindowOptions): Number {
        val webviewNode = viewTree.createNode(args)
        viewTree.appendTo(webviewNode)
        return webviewNode.id
    }
}

class ViewTree {
    val root = ViewTreeStruct(0, 0, mutableListOf())
    private val currentProcess = 0

    data class ViewTreeStruct(
        val id: Int,
        val processId: Int, //processId as parentId
        val children: MutableList<ViewTreeStruct?>
    )

    fun createNode(args: WindowOptions): ViewTreeStruct {
        var processId = currentProcess
        //  当用户传递了processId，即明确需要挂载到某个view下
        if (args.processId !== null) {
            processId = args.processId!!
        }
        return ViewTreeStruct(
            id = processId,
            processId = processId + 1, // self add node id
            children = mutableListOf()
            )
    }

    fun appendTo(webviewNode: ViewTreeStruct): Int {
        val processId =webviewNode.processId
        fun next(node:ViewTreeStruct): Int {
            // 找到加入节点
            if (node.processId == processId) {
                webviewNode.children.add(webviewNode)
                return webviewNode.id
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


}