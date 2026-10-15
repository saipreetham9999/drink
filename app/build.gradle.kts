import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.drink.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.drink.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    // A device needs only its own native runtime, not every phone/emulator ABI.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// MediaPipe's published core enables remote usage metrics. Ship only its no-op logger.
val mediapipeCoreArtifact by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}
val privateCoreAar = layout.buildDirectory.file("mediapipe-private/tasks-core-private.aar")
val preparePrivateMediaPipe by tasks.registering {
    inputs.files(mediapipeCoreArtifact)
    outputs.file(privateCoreAar)
    doLast {
        fun rewriteZip(bytes: ByteArray, classes: Boolean): ByteArray {
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { destination ->
                ZipInputStream(ByteArrayInputStream(bytes)).use { source ->
                    while (true) {
                        val entry = source.nextEntry ?: break
                        val name = entry.name
                        val removed = classes && listOf("TasksStatsLoggerFactory", "TasksStatsProtoLogger", "RemoteLoggingClient")
                            .any { name.startsWith("com/google/mediapipe/tasks/core/logging/$it") }
                        if (!removed) {
                            val contents = source.readBytes()
                            destination.putNextEntry(ZipEntry(name))
                            destination.write(if (!classes && name == "classes.jar") rewriteZip(contents, true) else contents)
                            destination.closeEntry()
                        }
                    }
                }
            }
            return output.toByteArray()
        }
        val target = privateCoreAar.get().asFile
        target.parentFile.mkdirs()
        target.writeBytes(rewriteZip(mediapipeCoreArtifact.singleFile.readBytes(), false))
    }
}

dependencies {
    mediapipeCoreArtifact("com.google.mediapipe:tasks-core:${libs.versions.mediapipe.get()}")
    implementation(libs.mediapipe.tasks.vision) { exclude(group = "com.google.mediapipe", module = "tasks-core") }
    implementation(files(privateCoreAar).builtBy(preparePrivateMediaPipe))
    // Non-telemetry core dependencies from its pinned POM; transport libraries are omitted.
    implementation("com.google.protobuf:protobuf-javalite:4.26.1")
    implementation("com.google.flogger:flogger:0.6")
    implementation("com.google.flogger:flogger-system-backend:0.6")
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
