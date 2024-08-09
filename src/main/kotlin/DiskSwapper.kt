import Utils.Companion.humanReadableByteCountSI
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import kotlin.math.max

class DiskSwapper(
    private val isVerbose: Boolean,
) {
    private val excludedDirectories = listOf(
        "System Volume Information",
        "\$RECYCLE.BIN",
    )

    private var drives: MutableList<File> = emptyList<File>().toMutableList()

    fun swap() {
        drives = File.listRoots().toMutableList()
        println("Disk Swapper, select two volumes to swap the contents of. This is not currently a sophisticated program, please only select simple data drives and backup liberally.")
        filterSystemDriveFromDrives()

        val driveA = getDriveToSwap("first")
        drives.remove(driveA)
        val driveB = getDriveToSwap("second")
        drives.remove(driveB)
        println("Attempting to swap the contents of drives $driveA and $driveB")

        validateSwap(driveA, driveB)

        if (driveA.canContain(driveB)) {
            doSimpleSwap(driveB, driveA)
        } else if (driveB.canContain(driveA)) {
            doSimpleSwap(driveA, driveB)
        } else {
            println("Not able to perform swap in this version of DiskSwapper. Neither partition has enough space to contain the other's data, at least one must have enough free space before starting.")
        }

        println("Disk Swap Completed!\n")
    }

    private fun filterSystemDriveFromDrives() {
        // We do not want to try to swap the system drive, it wouldn't end well.
        val systemDrive = System.getenv("SystemDrive")
        if (isVerbose) println("Filtering system drive from drive list: $systemDrive\n")
        drives.removeIf { drive -> drive.path.startsWith(systemDrive) }
    }

    private fun getDriveToSwap(enumeration: String): File {
        listAvailableDrives()
        println("Enter the number for the $enumeration Disk:\n")
        val firstDriveIndexString = readln()

        if (!validateDriveIndexInput(firstDriveIndexString)) {
            println("Entered value is not valid, enter a number e.g. '3'.")
            return getDriveToSwap(enumeration)
        }

        return drives[firstDriveIndexString.toInt()]
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

    private fun doSimpleSwap(
        driveA: File,
        driveB: File,
    ) {
        println("Performing simple swap of Drive contents between drives $driveA and $driveB")
        val driveAFileList = driveA.listFiles()?.toList() ?: emptyList<File>()
        val driveBFileList = driveB.listFiles()?.toList() ?: emptyList<File>()

        copyToDriveAndDeleteOriginalFiles(driveAFileList, driveB)
        copyToDriveAndDeleteOriginalFiles(driveBFileList, driveA)
    }

    private fun copyToDriveAndDeleteOriginalFiles(
        fileList: List<File>,
        drive: File,
    ) {
        for (entry: File in fileList) {
            if (excludedDirectories.contains(entry.name)) {
                println("Skipping ${entry.name}, restricted directory")
                continue
            }

            try {
                if (entry.isFile) {
                    if (isVerbose) println("Copying $entry to drive $drive")
                    FileUtils.copyFileToDirectory(entry, drive)
                    FileUtils.delete(entry)
                } else if (entry.isDirectory) {
                    if (isVerbose) println("Copying directory $entry and all contents to drive $drive")
                    FileUtils.copyDirectoryToDirectory(entry, drive)
                    FileUtils.deleteDirectory(entry)
                } else {
                    println("Different File type found, not copied, not deleted! - ${entry.path}")
                }
            } catch (e: Throwable) {
                println("Error encountered while copying $entry, file will not be copied. Error thrown: ${e.message}")
                continue
            }
        }
    }

    fun findLargestFileSizeUnderFolder(directory: File): Long {
        var largestFileSize = 0L
        val allFiles = directory.listFiles() ?: return 0L

        for (fileEntry: File in allFiles) {
            if (fileEntry.isDirectory) {
                largestFileSize = max(findLargestFileSizeUnderFolder(fileEntry), largestFileSize)
            } else {
                largestFileSize = max(Files.size(fileEntry.toPath()), largestFileSize)
            }
        }
        return largestFileSize
    }

    private fun validateSwap(
        driveA: File,
        driveB: File,
    ) {
        if (driveA == driveB) {
            throw IllegalArgumentException("Drives selected are the same, cannot swap. DriveA = $driveA, DriveB = $driveB")
        }

        val driveAUsedSize = driveA.totalSpace - driveA.freeSpace
        val driveBUsedSize = driveB.totalSpace - driveB.freeSpace

        if (driveAUsedSize >= driveB.totalSpace) {
            println("Not enough space available in drive $driveB, requires at least ${humanReadableByteCountSI(driveAUsedSize)} to perform swap, only ${humanReadableByteCountSI(driveB.totalSpace)} available in total.\nRestarting\n\n")
            swap()
        } else if (driveBUsedSize >= driveA.totalSpace) {
            println("Not enough space available in drive $driveA, requires at least ${humanReadableByteCountSI(driveBUsedSize)} to perform swap, only ${humanReadableByteCountSI(driveA.totalSpace)} available in total.\nRestarting\n\n")
            swap()
        }
    }

    private fun File.canContain(otherFile: File): Boolean {
        return this.usableSpace > (otherFile.totalSpace - otherFile.freeSpace)
    }
}