package info.bagen.rust.plaoc.util

import androidx.compose.runtime.*

@Stable
class IsChange {
    val changes: MutableState<Int> = mutableStateOf(0)
    private val policy by lazy {
        object : SnapshotMutationPolicy<Any?> {
            override fun equivalent(a: Any?, b: Any?): Boolean {
                return (a == b).also {
                    if (!it)
                        changes.value += 1
                }
            }
        }
    }


    fun <T> getPolicy(): SnapshotMutationPolicy<T> {
        return policy as SnapshotMutationPolicy<T>
    }

    @Composable
    fun effectChange(onChange: @Composable () -> Unit) {
        if (changes.value > 0) {
            changes.value = 0
            onChange()
        }
    }

    @Composable
    fun <T> rememberStateOf(value: T): MutableState<T> {
        return remember {
            mutableStateOf(value, getPolicy())
        }
    }
}

@Composable
inline fun rememberIsChange(): IsChange {
    return remember {
        IsChange()
    }
}
