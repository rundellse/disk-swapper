import Utils.Companion.humanReadableByteCountSI
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.math.max

class DiskSwapper(
    private val isVerbose: Boolean,
) {

    private val excludedDirectories = listOf(
        "System Volume Information",
        "\$RECYCLE.BIN",
    )
    private val cli = DiskSwapperCLI(isVerbose)

    fun swap() {
        println("Disk Swapper, select two volumes to swap the contents of. This is not currently (and likely will never be) a sophisticated program, please only use to swap simple data drives and backup liberally.")
        val driveA = cli.getDriveToSwap("first")
        val driveB = cli.getDriveToSwap("second")
        println("Attempting to swap the contents of drives $driveA and $driveB")

        if (!validateSwap(driveA, driveB)) {
            swap()
        }

        if (driveA.canContain(driveB)) {
            simpleSwap(driveB, driveA)
        } else if (driveB.canContain(driveA)) {
            simpleSwap(driveA, driveB)
        } else {
            println("Neither drive has enough free space to fully contain the other's data, attempting an alternating swap.")
            alternatingSwap(driveA, driveB)
        }

        println("Disk Swap Completed!\n")
    }


    private fun simpleSwap(
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
        // TODO: Align simple and alternating swap paths, they can almost certainly be aligned.
        for (entry: File in fileList) {
            if (excludedDirectories.contains(entry.name)) {
                println("Skipping ${entry.name}, restricted directory.")
                continue
            }

            try {
                if (entry.isFile) {
                    // Isolate drive character e.g. 'C' from "C:\", then replace with the destination drive's character. Keeps file path during transfer.
                    val destination = entry.parent.replaceBefore(':', drive.path.substringBefore(':'))
                    if (isVerbose) println("Copying $entry to $destination.")
                    FileUtils.copyFileToDirectory(entry, File(destination))
                    FileUtils.delete(entry)
                } else if (entry.isDirectory) {
                    if (isVerbose) println("Copying directory $entry and all contents to drive $drive.")
                    val listFiles = entry.listFiles()?.asList()
                    if (listFiles == null || listFiles.isEmpty()) {
                        FileUtils.copyDirectoryToDirectory(entry, drive)
                    } else {
                        copyToDriveAndDeleteOriginalFiles(listFiles, drive)
                    }
                    FileUtils.deleteDirectory(entry)
                } else {
                    println("Unknown File type found, not copied, not deleted! - ${entry.path}")
                }
            } catch (e: Throwable) {
                println("Error encountered while copying $entry, file will not be copied.")
                if (isVerbose) println("Error thrown: $e")
                continue
            }
        }
    }

    private fun alternatingSwap(
        driveA: File,
        driveB: File,
    ) {
        // List of all files ordered by size for both drives.
        val allFilesOrderedA = assembleSizeOrderedListOfFiles(driveA.listFiles()?.toList() ?: emptyList())
        val allFilesOrderedB = assembleSizeOrderedListOfFiles(driveB.listFiles()?.toList() ?: emptyList())

        // Then move files alternately, there should always be space now so should be unfettered.
        if (isVerbose) println("Performing alternating swap of files between ${driveA.path} and ${driveB.path}")
        doAlternatingSwap(allFilesOrderedA, allFilesOrderedB, driveA, driveB)

    }

    private fun doAlternatingSwap(
        fileListA: MutableList<File>,
        fileListB: MutableList<File>,
        driveA: File,
        driveB: File,
    ) {
        val maxNoOfFiles = max(fileListA.size, fileListB.size)
        for (i in 0 until maxNoOfFiles) {
            if (i < fileListA.size) {
                val fileAFile = fileListA[i]
                if (fileAFile.length() >= driveB.usableSpace) {
                    //TODO: This needs testing

                    // If the largest file of one side does not have space to move, try to move multiple files from the other drive until that space is exceeded.
                    makeSpaceByCopyingFilesBack(fileListB, fileAFile, driveA, driveB)
                }
                copyFileToDriveAndDelete(fileAFile, driveB)
            }
            if (i < fileListB.size) {
                val fileBFile = fileListB[i]
                if (fileBFile.length() >= driveA.usableSpace) {
                    // If the largest file of one side does not have space to move, try to move multiple files from the other drive until that space is exceeded.
                    makeSpaceByCopyingFilesBack(fileListA, fileBFile, driveB, driveA)
                }
                copyFileToDriveAndDelete(fileListB[i], driveA)
            }
        }
        for (i in 0 until maxNoOfFiles) {
            if (i < fileListA.size) removeFolder(fileListA[i])
            if (i < fileListB.size) removeFolder(fileListB[i])
        }
    }

    private fun makeSpaceByCopyingFilesBack(
        destinationDriveFileList: MutableList<File>,
        fileRequiringSpace: File,
        sourceDrive: File,
        destinationDrive: File
    ) {
        // Attempt to make space on driveB to accommodate the file
        val driveBSpaceMakingFileList = mutableListOf<File>()
        var driveBFileSizeSum = 0L
        for (driveBFile in destinationDriveFileList) {
            driveBFileSizeSum += driveBFile.length()
            driveBSpaceMakingFileList.add(driveBFile)

            if (driveBFileSizeSum >= fileRequiringSpace.length()) break
        }

        if (driveBFileSizeSum > sourceDrive.usableSpace) throw RuntimeException("Not enough room in drives to allow even an alternating file swap. Make space is at least one of the drives.")
        if (isVerbose) println("Moving files from $destinationDrive to $sourceDrive to make room for file $fileRequiringSpace, File is ${humanReadableByteCountSI(fileRequiringSpace.length())}, making $driveBFileSizeSum of space by moving ${driveBSpaceMakingFileList.size} files.")
        for (driveBFile in driveBSpaceMakingFileList) {
            copyFileToDriveAndDelete(driveBFile, sourceDrive)
        }
        destinationDriveFileList.removeAll(driveBSpaceMakingFileList)
    }

    private fun copyFileToDriveAndDelete(
        entry: File,
        destinationDrive: File,
    ) {
        // Isolate drive character e.g. 'C' from "C:\", then replace with the destination drive's character. Keeps file path during transfer.
        val destination = entry.parent.replaceBefore(':', destinationDrive.path.substringBefore(':'))
        // Directory is required to exist before transfer
        Files.createDirectories(Path(destination))

        if (entry.isFile) {
            if (isVerbose) println("Copying $entry to $destination.")

            FileUtils.copyFileToDirectory(entry, File(destination))
            FileUtils.delete(entry)
        } else if (entry.isDirectory) {
            val destinationDirectory = destination + File.separator + entry.name
            if (isVerbose) println("Creating directory $destinationDirectory if it is not already created.")
            Files.createDirectories(Path(destinationDirectory))
        } else {
            println("Unknown File type found, not to be copied or deleted! - ${entry.path}")
        }
    }

    private fun removeFolder(entry: File) {
        if (entry.isDirectory) {
            FileUtils.delete(entry)
        }
    }

    fun assembleSizeOrderedListOfFiles(listOfFiles: List<File>) : MutableList<File> {
        // Order by size descending, put directories at the end and then order them reverse-alphabetically to allow deletions to cascade up the file-tree.
        return assembleListOfFiles(listOfFiles).sortedBy { file ->
//            println("Comparing file $file. isDirectory: ${file.isDirectory}, totalSpace: ${file.length()}")
            if (file.isDirectory) {
                var charactersValueSum = 0L
                file.path.toCharArray().forEach { charactersValueSum += it.code }
                return@sortedBy Long.MAX_VALUE - charactersValueSum
            }
            else return@sortedBy file.length() * -1
        }.toMutableList()
    }

    private fun assembleListOfFiles(listOfFiles: List<File>) : MutableList<File> {
        val listOfAllFiles = mutableListOf<File>()

        for (entry: File in listOfFiles) {
            if (excludedDirectories.contains(entry.name)) {
                println("Skipping ${entry.name}, restricted directory.")
                continue
            }

            try {
                if (entry.isDirectory) {
                    listOfAllFiles.add(entry)
                    listOfAllFiles.addAll(
                        assembleListOfFiles(entry.listFiles()?.toList() ?: continue)
                    )
                } else if (entry.isFile) {
                    listOfAllFiles.add(entry)
                } else {
                   println("Cannot recognise ${entry.path} as file or directory, not including in list for swap or deletion.")
                }
            } catch (e: Throwable) {
                println("Error encountered while copying $entry, file will not be copied.")
                if (isVerbose) println("Error thrown: $e")
                continue
            }
        }

        return listOfAllFiles
    }

    private fun validateSwap(
        driveA: File,
        driveB: File,
    ) : Boolean {
        if (driveA == driveB) throw IllegalArgumentException("Drives selected are the same, cannot swap. DriveA = $driveA, DriveB = $driveB.")

        val driveAUsedSize = driveA.totalSpace - driveA.freeSpace
        val driveBUsedSize = driveB.totalSpace - driveB.freeSpace

        if (driveAUsedSize >= driveB.totalSpace) {
            println("Not enough space available in drive $driveB, requires at least ${humanReadableByteCountSI(driveAUsedSize)} to perform swap, only ${humanReadableByteCountSI(driveB.totalSpace)} available in total.\nRestarting\n\n")
            return false
        } else if (driveBUsedSize >= driveA.totalSpace) {
            println("Not enough space available in drive $driveA, requires at least ${humanReadableByteCountSI(driveBUsedSize)} to perform swap, only ${humanReadableByteCountSI(driveA.totalSpace)} available in total.\nRestarting\n\n")
            return false
        }
        return true
    }

    private fun File.canContain(otherFile: File): Boolean {
        return this.usableSpace > (otherFile.totalSpace - otherFile.freeSpace)
    }
}
