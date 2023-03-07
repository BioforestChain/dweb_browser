package info.bagen.rust.plaoc.microService.helper

open class AdapterManager<T> {
    private val adapterOrderMap = mutableMapOf<T, Int>()
    private var orderdAdapters = listOf<T>()
    val adapters get() = orderdAdapters
    fun append(order: Int = 0, adapter: T): (Unit) -> Boolean {
        adapterOrderMap[adapter] = order
        orderdAdapters =
            adapterOrderMap.toList().sortedBy { (_, b) -> b }.map { (adapter) -> adapter }
        return { remove(adapter) }
    }

    fun remove(adapter: T) = adapterOrderMap.remove(adapter) != null
}