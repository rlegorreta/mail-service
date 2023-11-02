import org.gradle.internal.classpath.Instrumented.systemProperty
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.21"
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.spring") version "1.8.21"
    kotlin("plugin.jpa") version "1.8.21"
    kotlin("kapt") version "1.8.21"
}

group = "com.ailegorreta"
version = "2.0.0"
description = "Micro service for sending emails received as Kafka events."

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal()
	mavenCentral()
     maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/" +
        project.findProperty("registryPackageUrl") as String? ?:
            System.getenv("URL_PACKAGE") ?:
            "rlegorreta/ailegorreta-kit")
        credentials {
            username = project.findProperty("registryUsername") as String? ?:
                    System.getenv("USERNAME") ?:
                    "rlegorreta"
            password = project.findProperty("registryToken") as String? ?: System.getenv("TOKEN")
        }
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

extra["springCloudVersion"] = "2022.0.3"
extra["testcontainersVersion"] = "1.18.1"
extra["otelVersion"] = "1.26.0"
extra["ailegorreta-kit-version"] = "2.0.0"
extra["alfresco-opencmis-extension-version"] = "0.7"
extra["chemistry-opencmis-version"] = "1.1.0"
extra["jsoup-version"] = "1.15.3"
extra["coroutines-version"] = "1.7.3"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")        // Reactive version

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client") {
        exclude(group = "org.springframework.cloud", module = "spring-cloud-starter-ribbon")
        exclude(group = "com.netflix.ribbon", module = "ribbon-eureka")
    }

    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka-streams")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines-version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutines-version")}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("org.jsoup:jsoup:${property("jsoup-version")}")      // HTML parsing templates

    implementation("org.alfresco.cmis.client:alfresco-opencmis-extension:${property("alfresco-opencmis-extension-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-client-api:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-client-impl:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-client-bindings:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-commons-api:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-commons-impl:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-osgi-client:${property("chemistry-opencmis-version")}")

    implementation("com.ailegorreta:ailegorreta-kit-commons-utils:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-resource-server-security:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-commons-event:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-commons-cmis:${property("ailegorreta-kit-version")}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
    // testImplementation("io.projectreactor:reactor-test")                // this is for web-flux testing
    testImplementation("com.squareup.okhttp3:mockwebserver")
    testImplementation("io.mockk:mockk:1.9.3")

    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.named<BootBuildImage>("bootBuildImage") {
    environment.set(environment.get() + mapOf("BP_JVM_VERSION" to "17.*"))
    imageName.set("ailegorreta/${project.name}")
    docker {
        publishRegistry {
            username.set(project.findProperty("registryUsername").toString())
            password.set(project.findProperty("registryToken").toString())
            url.set(project.findProperty("registryUrl").toString())
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        freeCompilerArgs += "-Xjvm-default=all-compatibility"            // needed to override default methods on interfaces
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
	useJUnitPlatform()
}
