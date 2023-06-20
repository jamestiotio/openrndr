package org.openrndr.draw

import java.nio.ByteBuffer

actual interface ShaderStorageBuffer {

    fun write(source: ByteBuffer, writeOffset: Int = 0)
    fun read(target: ByteBuffer, readOffset: Int = 0)
    actual val session: Session?
    actual val format: ShaderStorageFormat
    actual fun clear()
    actual fun bind(base: Int)
    actual fun destroy()

    actual fun put(elementOffset: Int, putter: BufferWriterStd430.() -> Unit): Int
    actual val shadow: ShaderStorageBufferShadow
    actual fun vertexBufferView(): VertexBuffer
}





