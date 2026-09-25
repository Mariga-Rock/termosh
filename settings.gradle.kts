pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Termosh"

include(":app")
include(":domain")

include(":core:common")
include(":core:security")
include(":core:database")
include(":core:datastore")
include(":core:ssh")
include(":core:mosh")
include(":core:terminal")
include(":core:service")
include(":core:ui")

include(":feature:servers")
include(":feature:terminal")
include(":feature:settings")
include(":feature:license")
