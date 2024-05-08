package ynsrc.examplefx.audiograph

import kotlin.math.abs

class CircularFloatBuffer(private val size: Int) {
    private val floatArray = FloatArray(size)
    private var head = 0
    private var tail = 0

    fun avaliable(): Int {
        return when {
            head == tail -> 0
            else -> abs(head - tail)
        }
    }

    fun put(f: Float) { floatArray[head++.mod(size)] = f }
    fun get() : Float = floatArray[tail++.mod(size)]
}