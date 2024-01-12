package org.node.array

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Float64Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.Uint32Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

fun ByteArray.toUint8Array() = Uint8Array(size).also {
  for ((i, byte) in withIndex()) {
    it[i] = byte
  }
}

fun ByteArray.toArrayBuffer() = toUint8Array().buffer
fun ByteArray.toInt8Array() = Int8Array(toArrayBuffer())
fun ByteArray.toInt16Array() = Int16Array(toArrayBuffer())
fun ByteArray.toInt32Array() = Int32Array(toArrayBuffer())
fun ByteArray.toUint16Array() = Uint16Array(toArrayBuffer())
fun ByteArray.toUint32Array() = Uint32Array(toArrayBuffer())
fun ByteArray.toFloat32Array() = Float32Array(toArrayBuffer())
fun ByteArray.toFloat64Array() = Float64Array(toArrayBuffer())

fun Uint8Array.toByteArray() = ByteArray(length) {
  this@toByteArray[it]
}

fun ArrayBuffer.toByteArray(offset: Int = 0, length: Int = this.byteLength) =
  Uint8Array(this, byteOffset = offset, length = length).toByteArray()

fun ArrayBufferView.toByteArray() = buffer.toByteArray(byteOffset, byteLength)