pluginManagement {

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    val quarkusVersion: String by settings
    plugins {
        id("io.quarkus") version quarkusVersion
    }

}


rootProject.name="BankingRestApi"
