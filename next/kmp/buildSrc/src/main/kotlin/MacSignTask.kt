import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.LinkOption
import javax.inject.Inject
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

abstract class MacSignTask : DefaultTask() {
  @get:Inject
  protected abstract val objects: ObjectFactory

  @get:Inject
  abstract val execOperations: ExecOperations

  @get:Inject
  internal abstract val fileOperations: FileSystemOperations

  @get:LocalState
  protected val workingDir: Provider<Directory> =
    project.layout.buildDirectory.dir("pinpit/tmp/pinpitCreateDefaultDistributableMacosX64")

  private val libsMappingFile: Provider<RegularFile> = workingDir.map {
    it.file("libs-mapping.txt")
  }

  private val libsMapping = FilesMapping()

  private val libsDir: Provider<Directory> = workingDir.map {
    it.dir("libs")
  }

  @get:LocalState
  protected val signDir: Provider<Directory> = project.layout.buildDirectory.dir("pinpit/tmp/sign")

  @get:OutputDirectory
  val logsDir: Provider<Directory> = project.layout.buildDirectory.dir("pinpit/tmp/log")

  @get:OutputDirectory
  val destinationDir: Provider<Directory> =
    project.layout.buildDirectory.dir("pinpit/binaries/main-default/macos/x64/distributableApp")

  @get:Input
  val verbose: Property<Boolean> = objects.notNullProperty(true)

  @get:Internal
  val javaRuntimeVersion: Property<Int> = objects.notNullProperty(17)

  @get:Internal
  val macPackageName: Property<String> = objects.property()

  @get:Internal
  val macBundleId: Property<String> = objects.property()

  @get:Internal
  val macEntitlementsFile: RegularFileProperty = objects.fileProperty()

  @get:Internal
  val macRuntimeEntitlementsFile: RegularFileProperty = objects.fileProperty()

  private val macSigner by lazy {
    val properties = project.localProperties()
    val identity = properties.getString("compose.desktop.mac.identity")
    val keychain = properties.getString("compose.desktop.mac.keychain")

    val validatedSettings =
      ValidatedMacOSSigningSettings(macBundleId.get(), identity, File(keychain), "", false)
    val runExternalTool = ExternalToolRunner(verbose, logsDir, execOperations)

    MacSignerImpl(validatedSettings, runExternalTool)
  }

  @TaskAction
  fun run() {
    initState()
    prepareWorkingDir()
    modifyRuntimeOnMacOsIfNeeded()
  }

  private fun initState() {
    val mappingFile = libsMappingFile.ioFile
    if (mappingFile.exists()) {
      try {
        libsMapping.loadFrom(mappingFile)
      } catch (e: Exception) {
        fileOperations.delete(mappingFile)
        throw e
      }
      logger.debug("Loaded libs mapping from {}", mappingFile)
    }
  }

  private fun jarCopyingProcessor(): FileCopyingProcessor {
    val tmpDirForSign = signDir.ioFile
    fileOperations.clearDirs(tmpDirForSign)
    return MacJarSignFileCopyingProcessor(macSigner, tmpDirForSign, javaRuntimeVersion.get())
  }

  private fun prepareWorkingDir() {
    val libsDir = libsDir.ioFile
    val fileProcessor = jarCopyingProcessor()

    fun copyFileToLibsDir(sourceFile: File, targetFile: File): File {
      fileProcessor.copy(sourceFile, targetFile)
      return targetFile
    }

    val appDir = destinationDir.ioFile.resolve("${macPackageName.get()}.app")
    val unsignLibsDir = appDir.resolve("Contents/app")

    unsignLibsDir.walk().forEach { sourceFile ->
      if (sourceFile.isJarFile) {
        val targetFile = sourceFile.copyTo(libsDir.resolve(sourceFile.name), true)
        sourceFile.delete()
        libsMapping[sourceFile] = listOf(copyFileToLibsDir(targetFile, sourceFile))
      }
    }

    fileOperations.clearDirs(libsDir)
  }


  private fun modifyRuntimeOnMacOsIfNeeded() {
    val appDir = destinationDir.ioFile.resolve("${macPackageName.get()}.app")
    val runtimeDir = appDir.resolve("Contents/runtime")

    val appEntitlementsFile = macEntitlementsFile.ioFileOrNull
    val runtimeEntitlementsFile = macRuntimeEntitlementsFile.ioFileOrNull


    // Resign the runtime completely (and also the app dir only)
    // Sign all libs and executables in runtime
    runtimeDir.walk().forEach { file ->
      val path = file.toPath()
      if (path.isRegularFile(LinkOption.NOFOLLOW_LINKS) && (path.isExecutable() || file.name.isDylibPath)) {
        macSigner.sign(file, runtimeEntitlementsFile)
      }
    }

    macSigner.sign(runtimeDir, runtimeEntitlementsFile, forceEntitlements = true)
    macSigner.sign(appDir, appEntitlementsFile, forceEntitlements = true)
  }
}

