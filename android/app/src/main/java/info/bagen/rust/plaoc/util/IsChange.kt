package info.bagen.rust.plaoc.util

import androidx.compose.runtime.*

@Stable
class IsChange(
    val needFirstCall: Boolean, _policy: SnapshotMutationPolicy<Any?>? = null
) {
    private val policy = _policy ?: structuralEqualityPolicy()
    val changes: MutableState<Int> = mutableStateOf(0)
    private val markChangePolicy by lazy {
        object : SnapshotMutationPolicy<Any?> {
            override fun equivalent(a: Any?, b: Any?): Boolean {
                return policy.equivalent(a, b).also {
                    if (!it) changes.value += 1
                }
            }
        }
    }


    fun <T> getPolicy(): SnapshotMutationPolicy<T> {
        return markChangePolicy as SnapshotMutationPolicy<T>
    }

    @Composable
    fun effectChange(onChange: @Composable () -> Unit) {
        if (changes.value > 0) {
            changes.value = 0
            onChange()
        }
    }

    @Composable
    inline fun <T> rememberToState(value: T): MutableState<T> {
        return remember(value) {
            if (needFirstCall) {
                changes.value += 1
            }
            mutableStateOf(value, getPolicy())
        }
    }

    @Composable
    inline fun <T> rememberByState(value: State<T>): State<T> {
        return remember {
            if (needFirstCall) {
                changes.value += 1
            }
            derivedStateOf(getPolicy()) {
                value.value
            }
        }
    }
}

@Composable
inline fun rememberIsChange(
    needFirstCall: Boolean, _policy: SnapshotMutationPolicy<Any?>? = null
): IsChange {
    return remember {
        IsChange(needFirstCall, _policy)
    }
}
