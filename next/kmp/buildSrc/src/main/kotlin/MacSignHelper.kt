import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.isExecutable

internal class MacJarSignFileCopyingProcessor(
  private val signer: MacSigner, private val tempDir: File, private val jvmRuntimeVersion: Int
) : FileCopyingProcessor {
  override fun copy(source: File, target: File) {
    if (source.isJarFile) {
      signNativeLibsInJar(source, target)
    } else {
      SimpleFileCopyingProcessor.copy(source, target)
      if (source.name.isDylibPath) {
        when {
          jvmRuntimeVersion < 17 -> signer.sign(target)
          /**
           * JDK 17 started to sign non-jar dylibs,
           * but it fails, when libs are already signed,
           * so we need to remove signature before running jpackage.
           *
           * JDK 18 processes signed libraries fine, so we don't have to do anything.
           *
           * Note that the JDK only signs dylib files and not jnilib files,
           * so jnilib files still need to be signed here.
           */
          jvmRuntimeVersion == 17 -> {
            if (source.name.endsWith(".jnilib")) {
              signer.sign(target)
            } else {
              signer.unsign(target)
            }
          }

          else -> {
            if (source.name.endsWith(".jnilib")) {
              signer.sign(target)
            }
          }
        }
      }
    }
  }

  private fun signNativeLibsInJar(source: File, target: File) {
    if (target.exists()) target.delete()

    transformJar(source, target) { entry, zin, zout ->

      if (entry.name.isDylibPath) {
        signDylibEntry(entry, zin, zout)
      } else {
        copyZipEntry(entry, zin, zout)
      }
    }
  }

  private fun signDylibEntry(sourceEntry: ZipEntry, zin: ZipInputStream, zout: ZipOutputStream) {
    val unpackedDylibFile = tempDir.resolve(sourceEntry.name.substringAfterLast("/"))
    try {
      zin.copyTo(unpackedDylibFile)
      signer.sign(unpackedDylibFile)
      unpackedDylibFile.inputStream().buffered().use {
        copyZipEntry(sourceEntry, from = it, to = zout)
      }
    } finally {
      unpackedDylibFile.delete()
    }
  }
}

object SimpleFileCopyingProcessor : FileCopyingProcessor {
  override fun copy(source: File, target: File) {
    source.copyTo(target, overwrite = true)
  }
}

internal interface FileCopyingProcessor {
  fun copy(source: File, target: File)
}


internal inline fun transformJar(
  sourceJar: File,
  targetJar: File,
  fn: (entry: ZipEntry, zin: ZipInputStream, zout: ZipOutputStream) -> Unit
) {
  ZipInputStream(FileInputStream(sourceJar).buffered()).use { zin ->
    ZipOutputStream(FileOutputStream(targetJar).buffered()).use { zout ->
      for (sourceEntry in generateSequence { zin.nextEntry }) {
        fn(sourceEntry, zin, zout)
      }
    }
  }
}

internal fun copyZipEntry(
  entry: ZipEntry,
  from: InputStream,
  to: ZipOutputStream,
) {
  val newEntry = ZipEntry(entry.name).apply {
    comment = entry.comment
    extra = entry.extra
  }
  to.withNewEntry(newEntry) {
    from.copyTo(to)
  }
}

internal inline fun ZipOutputStream.withNewEntry(zipEntry: ZipEntry, fn: () -> Unit) {
  putNextEntry(zipEntry)
  fn()
  closeEntry()
}

internal fun InputStream.copyTo(file: File) {
  file.outputStream().buffered().use { os ->
    copyTo(os)
  }
}

internal fun File.checkExistingFile(): File = apply {
  check(isFile) { "'$absolutePath' does not exist" }
}

internal val File.isJarFile: Boolean
  get() = name.endsWith(".jar", ignoreCase = true) && isFile


internal val String.isDylibPath
  get() = endsWith(".dylib") || endsWith(".jnilib")

internal abstract class MacSigner(protected val runTool: ExternalToolRunner) {
  /**
   * If [entitlements] file is provided, executables are signed with entitlements.
   * Set [forceEntitlements] to `true` to sign all types of files with the provided [entitlements].
   */
  abstract fun sign(
    file: File, entitlements: File? = null, forceEntitlements: Boolean = false
  )

  fun unsign(file: File) {
    runTool.unsign(file)
  }

  abstract val settings: ValidatedMacOSSigningSettings?
}

internal class MacSignerImpl(
  override val settings: ValidatedMacOSSigningSettings, runTool: ExternalToolRunner
) : MacSigner(runTool) {
  @Transient
  private var signKeyValue: String? = null

  override fun sign(
    file: File, entitlements: File?, forceEntitlements: Boolean
  ) {
    // sign key calculation is delayed to avoid
    // creating an external process during the configuration
    // phase, which became an error in Gradle 8.1
    // https://github.com/JetBrains/compose-multiplatform/issues/3060
    val signKey = signKeyValue ?: run {
      runTool(MacUtils.security, args = listOfNotNull(
        "find-certificate", "-a", "-c", settings.fullDeveloperID, settings.keychain?.absolutePath
      ), processStdout = { signKeyValue = matchCertificates(it) })
      signKeyValue!!
    }
    runTool.unsign(file)
    runTool.sign(
      file = file,
      signKey = signKey,
      entitlements = entitlements?.takeIf { forceEntitlements || file.isExecutable },
      prefix = settings.prefix,
      keychain = settings.keychain
    )
  }

  private fun matchCertificates(certificates: String): String {
    val regex = Pattern.compile("\"alis\"<blob>=\"([^\"]+)\"")
    val m = regex.matcher(certificates)
    if (!m.find()) {
      val keychainPath = settings.keychain?.absolutePath
      error(
        "Could not find certificate for '${settings.identity}'" + " in keychain [${keychainPath.orEmpty()}]"
      )
    }

    val result = m.group(1)
    if (m.find()) error(
      "Multiple matching certificates are found for '${settings.fullDeveloperID}'. " + "Please specify keychain containing unique matching certificate."
    )
    return result
  }
}


internal fun optionalArg(arg: String, value: String?): Array<String> =
  if (value != null) arrayOf(arg, value) else emptyArray()

private val File.isExecutable: Boolean
  get() = toPath().isExecutable()


internal val <T : FileSystemLocation> Provider<T>.ioFile: File
  get() = get().asFile

internal val <T : FileSystemLocation> Provider<T>.ioFileOrNull: File?
  get() = orNull?.asFile


internal data class ValidatedMacOSSigningSettings(
  val bundleID: String,
  val identity: String,
  val keychain: File?,
  val prefix: String,
  private val appStore: Boolean
) {
  val fullDeveloperID: String
    get() {
      val developerIdPrefix = "Developer ID Application: "
      val thirdPartyMacDeveloperPrefix = "3rd Party Mac Developer Application: "
      return when {
        identity.startsWith(developerIdPrefix) -> identity
        identity.startsWith(thirdPartyMacDeveloperPrefix) -> identity
        else -> (if (!appStore) developerIdPrefix else thirdPartyMacDeveloperPrefix) + identity
      }
    }
}

internal object MacUtils {
  val codesign: File by lazy {
    File("/usr/bin/codesign").checkExistingFile()
  }

  val security: File by lazy {
    File("/usr/bin/security").checkExistingFile()
  }

  val make: File by lazy {
    File("/usr/bin/make").checkExistingFile()
  }

  val open: File by lazy {
    File("/usr/bin/open").checkExistingFile()
  }

}

internal class MultiOutputStream(
  mainStream: OutputStream, private val secondaryStream: OutputStream
) : FilterOutputStream(mainStream) {
  override fun write(b: Int) {
    super.write(b)
    secondaryStream.write(b)
  }

  override fun flush() {
    super.flush()
    secondaryStream.flush()
  }

  override fun close() {
    try {
      super.close()
    } finally {
      secondaryStream.close()
    }
  }
}

internal fun OutputStream.alsoOutputTo(secondaryStream: OutputStream): OutputStream =
  MultiOutputStream(this, secondaryStream)


internal class ExternalToolRunner(
  private val verbose: Property<Boolean>,
  private val logsDir: Provider<Directory>,
  private val execOperations: ExecOperations
) {
  internal enum class LogToConsole {
    Always, Never, OnlyWhenVerbose
  }

  operator fun invoke(
    tool: File,
    args: Collection<String>,
    environment: Map<String, Any> = emptyMap(),
    workingDir: File? = null,
    checkExitCodeIsNormal: Boolean = true,
    processStdout: Function1<String, Unit>? = null,
    logToConsole: LogToConsole = LogToConsole.OnlyWhenVerbose,
    stdinStr: String? = null
  ): ExecResult {
    val logsDir = logsDir.ioFile
    logsDir.mkdirs()

    val toolName = tool.nameWithoutExtension
    val outFile = logsDir.resolve("${toolName}-${currentTimeStamp()}-out.txt")
    val errFile = logsDir.resolve("${toolName}-${currentTimeStamp()}-err.txt")

    val result = outFile.outputStream().buffered().use { outFileStream ->
      errFile.outputStream().buffered().use { errFileStream ->
        execOperations.exec {
          this.executable = tool.absolutePath
          this.args(*args.toTypedArray())
          workingDir?.let { wd -> this.workingDir(wd) }
          this.environment(environment)
          // check exit value later
          this.isIgnoreExitValue = true

          if (stdinStr != null) {
            this.standardInput = ByteArrayInputStream(stdinStr.toByteArray())
          }

          @Suppress("NAME_SHADOWING") val logToConsole = when (logToConsole) {
            LogToConsole.Always -> true
            LogToConsole.Never -> false
            LogToConsole.OnlyWhenVerbose -> verbose.get()
          }
          if (logToConsole) {
            this.standardOutput = this.standardOutput.alsoOutputTo(outFileStream)
            this.errorOutput = this.errorOutput.alsoOutputTo(errFileStream)
          } else {
            this.standardOutput = outFileStream
            this.errorOutput = errFileStream
          }
        }
      }
    }

    if (checkExitCodeIsNormal && result.exitValue != 0) {
      val errMsg = buildString {
        appendLine("External tool execution failed:")
        val cmd = (listOf(tool.absolutePath) + args).joinToString(", ")
        appendLine("* Command: [$cmd]")
        appendLine("* Working dir: [${workingDir?.absolutePath.orEmpty()}]")
        appendLine("* Exit code: ${result.exitValue}")
        appendLine("* Standard output log: ${outFile.absolutePath}")
        appendLine("* Error log: ${errFile.absolutePath}")
      }

      error(errMsg)
    }

    if (processStdout != null) {
      processStdout(outFile.readText())
    }

    if (result.exitValue == 0) {
      outFile.delete()
      errFile.delete()
    }

    return result
  }

  private fun currentTimeStamp() =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
}


private fun ExternalToolRunner.codesign(vararg args: String) =
  this(MacUtils.codesign, args.toList())

private fun ExternalToolRunner.unsign(file: File) =
  codesign("-vvvv", "--remove-signature", file.absolutePath)

private fun ExternalToolRunner.sign(
  file: File, signKey: String, entitlements: File?, prefix: String?, keychain: File?
) = codesign(
  "-vvvv",
  "--timestamp",
  "--options",
  "runtime",
  "--force",
  *optionalArg("--prefix", prefix),
  "--sign",
  signKey,
  *optionalArg("--keychain", keychain?.absolutePath),
  *optionalArg("--entitlements", entitlements?.absolutePath),
  file.absolutePath
)

internal fun FileSystemOperations.delete(vararg files: Any) {
  delete { delete(*files) }
}

internal fun FileSystemOperations.mkdirs(vararg dirs: File) {
  for (dir in dirs) {
    dir.mkdirs()
  }
}

internal fun FileSystemOperations.mkdirs(vararg dirs: Provider<out FileSystemLocation>) {
  mkdirs(*dirs.ioFiles())
}

internal fun FileSystemOperations.clearDirs(vararg dirs: File) {
  delete(*dirs)
  mkdirs(*dirs)
}

internal fun FileSystemOperations.clearDirs(vararg dirs: Provider<out FileSystemLocation>) {
  clearDirs(*dirs.ioFiles())
}

private fun Array<out Provider<out FileSystemLocation>>.ioFiles(): Array<File> =
  let { providers -> Array(size) { i -> providers[i].ioFile } }

// Serializable is only needed to avoid breaking configuration cache:
// https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:requirements
internal class FilesMapping : Serializable {
  private var mapping = HashMap<File, List<File>>()

  operator fun get(key: File): List<File>? =
    mapping[key]

  operator fun set(key: File, value: List<File>) {
    mapping[key] = value
  }

  fun remove(key: File): List<File>? =
    mapping.remove(key)

  fun loadFrom(mappingFile: File) {
    mappingFile.readLines().forEach { line ->
      if (line.isNotBlank()) {
        val paths = line.splitToSequence(File.pathSeparatorChar)
        val lib = File(paths.first())
        val mappedFiles = paths.drop(1).mapTo(ArrayList()) { File(it) }
        mapping[lib] = mappedFiles
      }
    }
  }

  fun saveTo(mappingFile: File) {
    mappingFile.parentFile.mkdirs()
    mappingFile.bufferedWriter().use { writer ->
      mapping.entries
        .sortedBy { (k, _) -> k.absolutePath }
        .forEach { (k, values) ->
          (sequenceOf(k) + values.asSequence())
            .joinTo(writer, separator = File.pathSeparator, transform = { it.absolutePath })
        }
    }
  }

  private fun writeObject(stream: ObjectOutputStream) {
    stream.writeObject(mapping)
  }

  private fun readObject(stream: ObjectInputStream) {
    @Suppress("UNCHECKED_CAST")
    mapping = stream.readObject() as HashMap<File, List<File>>
  }
}

internal inline fun <reified T : Any> ObjectFactory.notNullProperty(): Property<T> =
  property(T::class.java)

internal inline fun <reified T : Any> ObjectFactory.notNullProperty(defaultValue: T): Property<T> =
  property(T::class.java).value(defaultValue)

internal inline fun <reified T> ObjectFactory.new(vararg params: Any): T =
  newInstance(T::class.java, *params)