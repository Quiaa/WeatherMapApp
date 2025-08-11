import java.io.File
import java.util.Properties

// Create a Properties object to hold properties from local.properties
val localProperties = Properties()
// Define the path to the local.properties file using the correct root directory reference
val localPropertiesFile = File(rootDir, "local.properties")

// Check if the file exists and load it
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add Mapbox Maven repository
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                // Use public token for downloads
                username = "mapbox"
                // Directly get the property from the object we loaded above
                password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
            }
        }
    }
}

rootProject.name = "WeatherMapApp"
include(":app")