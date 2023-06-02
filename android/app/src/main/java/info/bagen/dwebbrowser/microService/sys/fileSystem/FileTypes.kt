package info.bagen.dwebbrowser.microService.sys.fileSystem

data class Fs(
    val name: String = "",
    val extname: String = "",
    val path: String = "",
    val cwd: String = "",
    val type: String = "",
    val isLink: Boolean = false,
    val relativePath: String = "",
)

data class FileLs(
    val path: String = "",
    val option: LsOption = LsOption()
)

data class LsOption(
  val filter: Array<LsFilter> = arrayOf(),
  val recursive: Boolean = false
)

enum class FileType(var value: String) { FILE(value = "file"), DIRECTORY(value = "directory") }

data class LsFilter(
    val type: String = "",
    val name: Array<String> = arrayOf()
)

data class FileRead(
    val path: String = ""
)

data class FileWrite(
    val path: String = "",
    val content: String = "",
    val option: WriteOption = WriteOption()
)

data class WriteOption(
    val append: Boolean = false,
    val autoCreate: Boolean = true,
)

data class FileRm(
    val path: String = "",
    val option: RmOption = RmOption()
)

data class FileRename(
    val path: String = "",
    val newPath: String = "",
)

data class RmOption(
    val deepDelete: Boolean = true
)

data class FileStat(
    val path: String = ""
)


