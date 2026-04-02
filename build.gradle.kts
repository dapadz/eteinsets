plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    `maven-publish`
}
tasks.register("printSigningProps") {
    doLast {
        val k = findProperty("signingKey") as String?
        val p = findProperty("signingPassword") as String?
        println("signingKey present: ${!k.isNullOrBlank()}  length=${k?.length}")
        println("signingPassword present: ${!p.isNullOrBlank()}  length=${p?.length}")
        if (!k.isNullOrBlank()) {
            println("signingKey head: " + k.take(40))
            println("signingKey tail: " + k.takeLast(40))
        }
    }
}
subprojects {
    plugins.withId("maven-publish") {
        publishing {
            repositories {
                maven {
                    name = "localBundle"
                    url = uri(rootProject.layout.buildDirectory.dir("local-maven"))
                }
            }
        }
    }
}