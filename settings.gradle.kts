pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }

  // Ensure all requests for Kotlin plugins use a single forced version to avoid
  // duplicate "kotlin" extension registration caused by multiple plugin versions.
  resolutionStrategy {
    eachPlugin {
      try {
        val idString = requested.id.id
        if (idString.startsWith("org.jetbrains.kotlin")) {
          useVersion("2.2.10")
        }
      } catch (e: Exception) {
        // Defensive: if requested.id or id.id isn't available for some plugin, ignore.
      }
    }
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "SEJAHTERA BERSAMA"

include(":app")
