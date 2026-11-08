import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import groovy.json.JsonOutput

val taskLocalProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "vn.edu.uth.todo"
    compileSdk = 35
    defaultConfig {
        applicationId = "vn.edu.uth.todo"
        minSdk = 24
        targetSdk = 35
        versionCode = 21
        versionName = "2.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TASKS_API_TOKEN", JsonOutput.toJson(
            System.getenv("TASKS_API_TOKEN") ?: taskLocalProperties.getProperty("tasks.api.token", "")))
        buildConfigField("String", "FIREBASE_DATABASE_URL", JsonOutput.toJson(
            System.getenv("FIREBASE_DATABASE_URL") ?: taskLocalProperties.getProperty("firebase.database.url",
                "https://to-do-lists-a558c-default-rtdb.asia-southeast1.firebasedatabase.app")))
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets.getByName("androidTest").assets.srcDir("src/test/resources")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.code.gson:gson:2.11.0")

    // Google Sign-In qua Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.facebook.android:facebook-login:19.0.0")

    // Dùng await() với Firebase Task
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}

// Keep JUnit worker classpaths readable by Java on Vietnamese Windows paths.
tasks.withType<Test>().configureEach {
    if (System.getProperty("os.name").startsWith("Windows") && rootDir.path.any { it.code > 127 }) {
        doFirst {
            val manifest = Manifest().apply {
                mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
                mainAttributes[Attributes.Name.CLASS_PATH] =
                    classpath.files.joinToString(" ") { it.toURI().toASCIIString() }
            }
            val directory = gradle.gradleUserHomeDir.resolve("unicode-test-classpaths").apply { mkdirs() }
            val pathJar = directory.resolve("uth-todo-${rootDir.path.hashCode()}-$name.jar")
            JarOutputStream(pathJar.outputStream(), manifest).use { }
            classpath = files(pathJar)
        }
    }
}
