package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("MultiWebViewNMM", tag, msg, err)


class MultiWebViewNMM : NativeMicroModule("mwebview.sys.dweb") {
    companion object {

        val activityMap = mutableMapOf<Mmid, PromiseOut<MutilWebViewActivity>>()
    }

    override suspend fun _bootstrap() {
        // 打开webview

        val query_url = Query.string().required("url")
        val query_dwebHost = Query.string().optional("dweb-host")
        val query_webviewId = Query.string().required("webview_id")

        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request, ipc ->
                // 接收process_id 用于区分应用内多页面，如果传递process_id 就是要去打开旧页面
                val url = query_url(request)
                val dwebHost = query_dwebHost(request)
                openDwebView(ipc.remote, url, dwebHost)
            },
            "/close" bind Method.GET to defineHandler { request, ipc ->
                val webviewId = query_webviewId(request)
                val remoteMmid = ipc.remote.mmid

                closeDwebView(remoteMmid, webviewId)
            })
    }

    override suspend fun _shutdown() {
        apiRouting = null
    }

    private var viewTree: ViewTree = ViewTree()


    @Synchronized
    private fun openMutilWebViewActivity(remoteMmid: Mmid) = activityMap.getOrPut(remoteMmid) {
        debugMultiWebView("OPEN-ACTIVITY", "remote-mmid: $remoteMmid")
        App.startActivity(MutilWebViewActivity::class.java) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val b = Bundle();
            b.putString("mmid", remoteMmid);
            intent.putExtras(b);
        }
        PromiseOut()
    }

    private suspend fun openDwebView(
        remoteMm: MicroModule,
        url: String,
        dwebHost: String?
    ): String {
        val remoteMmid = remoteMm.mmid
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / url:$url")
        val activity = openMutilWebViewActivity(remoteMmid).waitPromise()
        return activity.openWebView(remoteMm, url, dwebHost)
    }

    private suspend fun closeDwebView(remoteMmid: String, webviewId: String): Boolean {
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / webview-id:$webviewId")
        val activity = activityMap[remoteMmid]?.waitPromise()
            ?: throw Exception("no found activity for mmid: $remoteMmid")

        return activity.closeWebView(webviewId)
    }
}

/*
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
        return webViewNode.id*/

class ViewTree {
    private val root = ViewTreeStruct(0, 0, "", mutableListOf())
    private var currentProcess = 0

    data class ViewTreeStruct(
        val id: Int, val processId: Int, //processId as parentId
        val origin: String, val children: MutableList<ViewTreeStruct?>
    )

    fun createNode(origin: String, processId: String?): ViewTreeStruct {
        // 当前要挂载到哪个父级节点
        var cProcessId = currentProcess
        //  当用户传递了processId，即明确需要挂载到某个view下
        if (!processId.isNullOrEmpty()) {
            cProcessId = processId.toInt()
        }
        return ViewTreeStruct(
            id = cProcessId + 1, processId = cProcessId, // self add node id
            origin = origin, children = mutableListOf()
        )
    }

    fun appendTo(webViewNode: ViewTreeStruct): Int {
        val processId = webViewNode.processId
        fun next(node: ViewTreeStruct): Int {
            // 找到加入节点
            if (node.id == processId) {
                // 因为节点已经加入了，所以当前节点进程移动到新创建的节点
                currentProcess = webViewNode.id
//                println("multiWebView#currentProcess:$currentProcess")
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
