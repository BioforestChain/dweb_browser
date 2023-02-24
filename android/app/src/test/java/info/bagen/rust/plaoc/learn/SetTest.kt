package info.bagen.rust.plaoc.learn

import org.junit.jupiter.api.Test

class SetTest {
    @Test
    fun iterRemove() {
        val set = mutableListOf(1, 2, 3, 4, 5, 6)
        val iterator = set.listIterator()
        for (item in iterator) {
            if (item == 1) {
                iterator.remove()
            } else if (item == 3) {
                iterator.remove()
            } else if (item == 5) {
                set.add(7)
            }
            println(item)
        }


        println(set)
    }
}