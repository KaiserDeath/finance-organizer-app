pluginManagement {
    includeBuild("build-logic")
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
    }
}

rootProject.name = "MoneyFlow"

include(":app")

include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

include(":feature:dashboard")
include(":feature:transactions")
include(":feature:addedit")
include(":feature:categories")
include(":feature:paymentmethods")
include(":feature:budgets")
include(":feature:upcoming")
include(":feature:recurring")
include(":feature:analytics")
include(":feature:accounts")
include(":feature:savings")
include(":feature:currency")
