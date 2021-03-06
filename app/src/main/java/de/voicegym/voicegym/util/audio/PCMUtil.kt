package de.voicegym.voicegym.util.audio

fun getDoubleArrayFromShortArray(normalization: Double, inputArray: ShortArray): DoubleArray {
    val outputArray = DoubleArray(inputArray.size)
    for (i in 0 until inputArray.size) {
        if (inputArray[i] >= 0) {
            outputArray[i] = normalization * (inputArray[i].toDouble() / Short.MAX_VALUE)
        } else {
            outputArray[i] = -normalization * (inputArray[i].toDouble() / Short.MIN_VALUE)
        }
    }
    return outputArray
}
