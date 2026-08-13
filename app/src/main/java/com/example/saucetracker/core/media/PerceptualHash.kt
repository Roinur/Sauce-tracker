package com.example.saucetracker.core.media

import android.graphics.Bitmap
import android.graphics.Color

internal fun computeDHash64(bitmap: Bitmap): Long {
    val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
    var hash = 0L
    var bit = 0
    try {
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = scaled.getPixel(x, y)
                val right = scaled.getPixel(x + 1, y)
                val leftLum = 299 * Color.red(left) + 587 * Color.green(left) + 114 * Color.blue(left)
                val rightLum = 299 * Color.red(right) + 587 * Color.green(right) + 114 * Color.blue(right)
                if (leftLum >= rightLum) hash = hash or (1L shl bit)
                bit += 1
            }
        }
    } finally {
        if (scaled !== bitmap && !scaled.isRecycled) scaled.recycle()
    }
    return hash
}
