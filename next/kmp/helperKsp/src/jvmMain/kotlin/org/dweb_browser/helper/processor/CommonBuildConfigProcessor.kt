package org.dweb_browser.helper.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.File
import java.io.OutputStream
import java.util.Properties

class CommonBuildConfigProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String>
) : SymbolProcessor {
  private var invoked = false
  override fun process(resolver: Resolver): List<KSAnnotated> {
    // 判断编译的时候是否传入了 -PreleaseBuild=true，表示是脚本执行
    val isReleaseBuild = options["releaseBuild"] != null && options["releaseBuild"] == "true"

    logger.warn("CommonBuildConfigProcessor: isReleaseBuild=$isReleaseBuild")

    if (invoked) {
      return emptyList()
    }
    invoked = true

    val localProperties = Properties()
    val localPropertiesFile = File(options["rootDir"], "local.properties")
    if (localPropertiesFile.exists()) {
      localProperties.load(localPropertiesFile.inputStream())
    }
    val keyValuePairCodes = when {
      isReleaseBuild -> emptyList()
      else -> localProperties.mapNotNull { (key, value) ->
        val keyStr = key.toString()
        val valStr = value.toString()
        // 只针对 dweb- 开头的内容
        when {
          keyStr.startsWith("dweb-") && valStr.isNotBlank() -> {
            """
              "$keyStr" to "$valStr"
            """.trimIndent()
          }

          else -> null
        }
      }
    }

    val packageName = "org.dweb_browser.helper"
    val file: OutputStream = codeGenerator.createNewFile(
      dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
      packageName = packageName,
      fileName = "CommonBuildConfig"
    )
    val fileContent = """
package $packageName

public object CommonBuildConfig {
  public val switchMaps: Map<String, String> = mapOf(
    ${keyValuePairCodes.joinToString(",\n        ")}
  )
}
    """.trimIndent()

    file += fileContent
    file.close()

    return emptyList()
  }

  operator fun OutputStream.plusAssign(str: String) {
    this.write(str.toByteArray())
  }

}

class CommonBuildConfigProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return CommonBuildConfigProcessor(
      environment.codeGenerator,
      environment.logger,
      environment.options
    )
  }
}