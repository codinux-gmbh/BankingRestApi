plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.allopen") version "1.5.31"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}


group = "net.codinux.banking"
version = "1.0.0-SNAPSHOT"


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


val quarkusVersion: String by project
val fints4jVersion = "1.0.0-Alpha-9"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-jacoco")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-resteasy-jackson")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-smallrye-openapi")

    implementation("net.dankito.banking:fints4k-jvm6:$fints4jVersion")
    implementation("net.dankito.banking:BankFinder-jvm:$fints4jVersion")
    implementation("com.github.hbci4j:hbci4j-core:3.1.55")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured:4.4.0")
    testImplementation("org.assertj:assertj-core:3.21.0")
}


allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.javaParameters = true
}



/*      configures publishing to Maven Central / local      */

val commonScriptsFile = File(File(project.gradle.gradleUserHomeDir, "scripts"), "commonScripts.gradle")
if (commonScriptsFile.exists()) {
    apply(from = commonScriptsFile)
}


ext["artifactName"] = "banking-rest-api"

ext["useNewSonatypeRepo"] = true
ext["packageGroup"] = "net.codinux"

ext["sourceCodeRepositoryBaseUrl"] = "https://github.com/codinux-gmbh/BankingRestApi"

ext["developerId"] = "codinux"
ext["developerName"] = "codinux GmbH & Co. KG"
ext["developerMail"] = "git@codinux.net"

ext["licenseName"] = "<not_specified_yet>"
ext["licenseUrl"] = "<not_specified_yet>"


tasks.withType<GenerateModuleMetadata> {
    // to suppress error: Variant 'runtimeElements' contains a dependency on enforced platform 'io.quarkus.platform:quarkus-bom'
    suppressedValidationErrors.add("enforced-platform")
}
