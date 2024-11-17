import java.text.CharacterIterator
import java.text.StringCharacterIterator

class Utils {
    companion object {
        fun humanReadableByteCountSI(bytesLong: Long): String {
            var bytes = bytesLong
            if (-1000 < bytes && bytes < 1000) {
                return "$bytes B"
            }
            val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
            while (bytes <= -999950 || bytes >= 999950) {
                bytes /= 1000
                ci.next()
            }
            return String.format("%.1f %cB", bytes / 1000.0, ci.current())
        }
    }
}
