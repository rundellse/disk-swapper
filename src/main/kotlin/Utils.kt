import java.text.CharacterIterator
import java.text.StringCharacterIterator

class Utils {
    companion object {

        /**
         * Create a human-readable String output of data-size from a given number of bytes.
         * Completely and shamelessly copied from https://stackoverflow.com/a/3758880, as it's excellent.
         *
         * @param bytesLong - Number of bytes to make human-readable
         * @return - Human-readable presentation of the number of bytes as String.
         */
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
