package ynsrc.examplefx.audiograph

import kotlin.math.abs

class CircularShortBuffer(private val size: Int) {
    private val floatArray = ShortArray(size)
    private var head = 0
    private var tail = 0

    fun available(): Int {
        return when {
            head == tail -> 0
            else -> abs(head - tail)
        }
    }

    fun put(f: Short) { floatArray[head++.mod(size)] = f }
    fun get() : Short = floatArray[tail++.mod(size)]
    fun size() = floatArray.size
}