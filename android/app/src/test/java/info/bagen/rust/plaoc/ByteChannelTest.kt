package info.bagen.rust.plaoc

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ByteChannelTest {
    val bytechannel = ByteChannel(true)


    fun log(log: String) = println("${LocalTime.now()}\t\t$log")

    @Test
    fun test1() = runBlocking {
        GlobalScope.launch {
            var i = 0;
            while (i < 10) {
                delay(500)
                log("channel write $i")
                bytechannel.writeInt(i++)
            }
            bytechannel.close()
        }



//        bytechannel.readRemaining(4)
//        while (!bytechannel.isClosedForRead) {
//
//            bytechannel.readAvailable(4) {
//                log("channel readed ${it.int}")
//            }
//            bytechannel.readInt()
//            bytechannel.readRemaining(4).readBytes().toInt()
//            log("channel reading")
//            val byte = bytechannel.readRemaining(4).readBytes().toInt()
//            log("channel readed $byte")
//        }

        val inputStream = bytechannel.toInputStream()

        while (inputStream.available()>0){
//            inputStream.
        }

        log("done ${bytechannel.isClosedForRead}")
        Assertions.assertTrue(bytechannel.isClosedForRead)

    }

    @Test
    fun test2() = runBlocking {

    }


}