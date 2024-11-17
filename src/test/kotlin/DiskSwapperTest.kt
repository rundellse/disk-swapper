import io.kotest.core.spec.style.FunSpec
import java.io.File

class DiskSwapperTest : FunSpec({

    class TestFile(
        val pathname: String,
        totalSpace: Long,
        freeSpace: Long,
        isFile: Boolean,
        isDirectory: Boolean,
    ) : File(pathname) {
        val tSpace = totalSpace
        val fSpace = freeSpace
        val file = isFile
        val directory = isDirectory

        override fun getTotalSpace() : Long {
            return this.tSpace
        }
        override fun getFreeSpace() : Long {
            return this.fSpace
        }
        override fun isFile() : Boolean {
            return this.file
        }
        override fun isDirectory() : Boolean {
            return this.directory
        }

    }

    val diskSwapper = DiskSwapper(true)

    test("test test") {
        val mockDrive1 = TestFile("TestDrive1", 100L, 20L, isFile = false, isDirectory = true)
        val mockDrive2 = TestFile("TestDrive2", 100L, 20L, isFile = false, isDirectory = true)

        diskSwapper.assembleSizeOrderedListOfFiles(listOf(mockDrive1, mockDrive2))
    }

//    val diskSwapperSpy = spyk(DiskSwapper(true))
//
//    fun createMockDrive(
//        path: String,
//        totalSpace: Long,
//        freeSpace: Long,
//    ) : File {
//        val mockDrive = mockk<File>()
//        every { mockDrive.path } returns "D:\\"
//        every { mockDrive.totalSpace } returns 9999L
//        every { mockDrive.freeSpace } returns 999L
//        return mockDrive
//    }
//
//    test("Validation test - Insufficient total space for exchange") {
//        val mockDriveD = createMockDrive("D:\\", 9999L, 999L)
//        val mockDriveE = createMockDrive("E:\\", 8888L, 888L)
//
//        diskSwapperSpy.validateSwap(mockDriveD, mockDriveE) shouldBe false
//    }
//    test("Validation test - Insufficient space for exchange") {
//        val mockDriveD = createMockDrive("D:\\", 7777L, 999L)
//        val mockDriveE = createMockDrive("E:\\", 8888L, 888L)
//
//        diskSwapperSpy.validateSwap(mockDriveD, mockDriveE) shouldBe false
//    }
})
