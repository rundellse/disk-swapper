import Utils.Companion.humanReadableByteCountSI
import java.io.File

class DiskSwapperCLI(
    private val isVerbose: Boolean,
) {
    private val drives = File.listRoots().toMutableList()
    init {
        filterSystemDriveFromDrives()
    }




    fun filterSystemDriveFromDrives() {
        // We do not want to try to swap the system drive, it wouldn't end well.
        val systemDrive = System.getenv("SystemDrive")
        if (isVerbose) println("Filtering system drive from drive list: $systemDrive\n")
        drives.removeIf { drive -> drive.path.startsWith(systemDrive) }
    }

    fun getDriveToSwap(enumeration: String): File {
        listAvailableDrives()
        println("Enter the number for the $enumeration Disk:\n")
        val firstDriveIndexString = readln()

        if (!validateDriveIndexInput(firstDriveIndexString)) {
            println("Entered value is not valid, enter a number e.g. '3'.")
            return getDriveToSwap(enumeration)
        }


        val chosenDrive = drives[firstDriveIndexString.toInt()]
        drives.remove(chosenDrive)
        return chosenDrive
    }

    private fun validateDriveIndexInput(driveIndexString: String) : Boolean {
        val driveIndex = driveIndexString.toIntOrNull() ?: return false

        if (driveIndex < 0 || driveIndex >= drives.size) return false

        return true
    }

    private fun listAvailableDrives() {
        println("Available drives, please select one of the following.\n")
        for (drive: File in drives) {
            println("${drives.indexOf(drive)}: ${drive.path} " +
                    "| Total Space: ${humanReadableByteCountSI(drive.totalSpace)} " +
                    "| Used Space: ${humanReadableByteCountSI(drive.totalSpace - drive.freeSpace)} " +
                    "| Free Space: ${humanReadableByteCountSI(drive.freeSpace)}\n")
        }
    }
}