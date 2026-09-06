plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.4.0"
}

group = "com.backend"
version = "0.0.1-SNAPSHOT"
description = "travelervpn"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.4.0"

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0"))

    implementation("org.springframework.boot:spring-boot-starter-cassandra")
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra-reactive")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-cassandra")
    implementation("com.ing.data:cassandra-jdbc-wrapper:5.0.2")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-cio:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-jackson:${ktorVersion}")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.maxmind.geoip2:geoip2:4.2.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-starter-cassandra-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-cassandra-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.platform:junit-platform-console")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/resources/api-schema.json")
    outputDir.set("${buildDir}/generated/openapi")
    apiPackage.set("com.backend.travelervpn.generated.api")
    modelPackage.set("com.backend.travelervpn.generated.api.schema")

    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "serializationLibrary" to "jackson",
        "library" to "jvm-ktor"
    ))
}

sourceSets {
    main {
        kotlin.srcDir("${buildDir}/generated/openapi/src/main/kotlin")
    }
}

val fixKtorInternalApiTask = tasks.register("fixKtorInternalApi") {
    dependsOn(tasks.openApiGenerate)

    val targetDir = file("$buildDir/generated/openapi")

    inputs.dir(targetDir)
    outputs.dir(targetDir)

    doLast {
        if (targetDir.exists()) {
            targetDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "kt") {
                    var content = file.readText()

                    if (content.contains("import io.ktor.util.InternalAPI")) {
                        content = content.replace(
                            "import io.ktor.util.InternalAPI",
                            "import io.ktor.utils.io.InternalAPI"
                        )
                        file.writeText(content)
                        logger.lifecycle("Fixed InternalAPI import in file: ${file.name}")
                    }
                }
            }
        }
    }
}

tasks.compileKotlin {
    dependsOn(fixKtorInternalApiTask)
}

tasks.withType<Test> {
    useJUnitPlatform()
}