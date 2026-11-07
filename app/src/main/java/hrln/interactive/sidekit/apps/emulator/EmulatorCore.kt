package hrln.interactive.sidekit.apps.emulator

import android.graphics.Bitmap

internal object EmulatorCore {
    init { System.loadLibrary("sidekit_emulator") }
    external fun open(path: String): Long
    external fun dimensions(handle: Long): Int
    external fun frame(handle: Long, keys: Int, bitmap: Bitmap, audio: ShortArray): Int
    external fun save(handle: Long): ByteArray
    external fun restore(handle: Long, bytes: ByteArray): Boolean
    external fun close(handle: Long)
}
