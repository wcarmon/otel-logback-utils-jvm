import com.diffplug.gradle.spotless.SpotlessExtension

val mvnGroupId = "io.github.wcarmon"
val mvnArtifactId = "otel-logback-utils-jvm" // see settings.gradle.kts
val mvnVersion = "1.0.4"

val ossrhPassword: String = providers.gradleProperty("ossrhPassword").getOrElse("")
val ossrhUsername: String = providers.gradleProperty("ossrhUsername").getOrElse("")

repositories {
    mavenCentral()
}

plugins {
    java
    id("com.diffplug.spotless") version "6.23.3"

    `java-library`
    `maven-publish`
    signing
}

group = mvnGroupId
version = mvnVersion

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {

    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.opentelemetry:opentelemetry-api:1.33.0")
    implementation("io.opentelemetry:opentelemetry-context:1.33.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.33.0")
    implementation("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Test>().configureEach {

    jvmArgs("--enable-preview")

    failFast = true // marks others after failure as ignored
    useJUnitPlatform {
        excludeTags.add("e2e")
    }

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showStandardStreams = true
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = mvnGroupId
            artifactId = mvnArtifactId
            version = mvnVersion

            from(components["java"])

            suppressPomMetadataWarningsFor("runtimeElements")

            versionMapping {

            }

            pom {
                name = mvnArtifactId
                description = "Utilities for using OpenTelemetry"
                url = "https://github.com/wcarmon/otel-utils-jvm"

                licenses {
                    license {
                        name = "MIT License"
                        url =
                            "https://raw.githubusercontent.com/wcarmon/otel-utils-jvm/main/LICENSE"
                    }
                }

                developers {
                    developer {
                        email = "github@wcarmon.com"
                        id = "wcarmon"
                        name = "Wil Carmon"
                        organization = ""
                    }
                }

                scm {
                    connection =
                        "scm:git:git@github.com:wcarmon/otel-utils-jvm.git"
                    developerConnection =
                        "scm:git:ssh://github.com:wcarmon/otel-utils-jvm.git"
                    url = "https://github.com/wcarmon/otel-utils-jvm/tree/main"
                }
            }
        }
    }

    repositories {
        maven {

            // -- See ~/.gradle/gradle.properties
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }

            val releasesRepoUrl =
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            val snapshotsRepoUrl = uri(layout.buildDirectory.dir("repos/snapshots")) // TODO: fix

            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl
            else releasesRepoUrl // TODO: fix
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

configure<SpotlessExtension> {
    java {
        googleJavaFormat("1.18.1").aosp().reflowLongStrings()
        importOrder()
        removeUnusedImports()

        target(
            "src/*/java/**/*.java"
        )

        targetExclude(
            "src/gen/**",
            "src/genTest/**"
        )
    }
}
