plugins {
  id("kmp-library")
  id("target-compose")
  kotlin("plugin.serialization")
}

/*
 * bug: desktopProcessResources is a duplicate but no duplicate handling strategy has been set
 * see: https://docs.gradle.org/8.6/dsl/org.gradle.api.tasks.Copy.html#org.gradle.api.tasks.Copy:duplicatesStrategy
 * fix: https://youtrack.jetbrains.com/issue/KT-68566/processResources-Gradle-task-fails-with-Entry-application-test.yml-is-a-duplicate-but-no-duplicate-handling-strategy-has-been
 */
tasks.withType<ProcessResources> {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
}