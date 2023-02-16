package info.bagen.rust.plaoc.microService.helper

import java.util.*


fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

fun String.asBase64(): ByteArray = Base64.getDecoder().decode(this)