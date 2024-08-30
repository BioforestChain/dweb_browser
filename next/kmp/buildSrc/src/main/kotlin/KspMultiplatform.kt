import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.kspAndroid(dependencyNotation: Any) {
  add("kspAndroid", dependencyNotation)
}

fun DependencyHandlerScope.kspDesktop(dependencyNotation: Any) {
  add("kspDesktop", dependencyNotation)
}

fun DependencyHandlerScope.kspIos(dependencyNotation: Any) {
  add("kspIosX64", dependencyNotation)
  add("kspIosArm64", dependencyNotation)
  add("kspIosSimulatorArm64", dependencyNotation)
}

fun DependencyHandlerScope.kspAll(dependencyNotation: Any) {
  add("kspCommonMainMetadata", dependencyNotation)
//  kspAndroid(dependencyNotation)
//  kspDesktop(dependencyNotation)
//  kspIos(dependencyNotation)
}

