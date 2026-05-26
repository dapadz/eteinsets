import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    signing
}

android {
    namespace = "ru.dapadz.eteinsets"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "ru.dapadz"
                artifactId = "eteinsets"
                version = "1.1.0"

                from(components["release"])

                pom {
                    name.set("ETEInsets")
                    description.set("Insets helpers for Android")
                    url.set("https://github.com/dapadz/eteinsets")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    scm {
                        url.set("https://github.com/dapadz/eteinsets")
                        connection.set("scm:git:https://github.com/dapadz/eteinsets.git")
                        developerConnection.set("scm:git:ssh://git@github.com:dapadz/eteinsets.git")
                    }

                    developers {
                        developer {
                            id.set("dapadz")
                            name.set("dapadz")
                            email.set("dapadz@vk.com")
                        }
                    }
                }
            }
        }
    }
    signing {
        val keyFilePath = localProperties.getProperty("signingKeyFile")
        val password = localProperties.getProperty("signingPassword")

        require(!keyFilePath.isNullOrBlank()) {
            "Property 'signingKeyFile' is missing in local.properties"
        }
        require(!password.isNullOrBlank()) {
            "Property 'signingPassword' is missing in local.properties"
        }

        val keyText = file(keyFilePath).readText(Charsets.UTF_8)

        useInMemoryPgpKeys(keyText, password)
        sign(publishing.publications["maven"])
    }

    tasks.withType<Sign>().configureEach {
        doFirst {
            println("SIGN TASK: $path, signatory=" + (project.extensions.getByType(SigningExtension::class.java).signatory))
        }
    }

}
