package info.bagen.rust.plaoc.microService.helper

import io.ktor.utils.io.*


 class ByteChannelOut {
    val stream = ByteChannel()

    fun pull(){
        this._on_pull_signal?.emit {  } // TODO 这样好像有问题
    }

    private var _on_pull_signal: Signal<OnPull>? = null;
    fun onPull(): Signal<() -> Unit> {
        return (this._on_pull_signal ?: Signal<OnPull>());
    }
}
typealias OnPull = () -> Unit;
