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
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)  // Ensure it prefers settings repositories
    repositories {
        google()
        mavenCentral()
        // jcenter() // Uncomment if necessary
    }
}

rootProject.name = "Paaltjes voetbal"
include(":app")