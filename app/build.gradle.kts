import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.otiniel.copy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.otiniel.copy"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    // The local model is already quantized. Keeping it uncompressed makes the
    // APK faster to build/install and lets AssetManager expose its exact length.
    androidResources {
        noCompress += "litertlm"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Keep Compose artifacts compatible with Android Gradle Plugin 8.11 / API 36.
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // On-device LLM runtime. Inference runs locally; no API key or cloud endpoint required.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

val bundledModelFile = layout.projectDirectory.file(
    "src/main/assets/models/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
)

val downloadBundledLocalAiModel by tasks.registering {
    outputs.file(bundledModelFile)
    doLast {
        val target = bundledModelFile.asFile
        if (target.exists() && target.length() > 250_000_000L) {
            logger.lifecycle("COPY local model already present: ${target.length()} bytes")
            return@doLast
        }

        target.parentFile.mkdirs()
        val partial = target.resolveSibling(target.name + ".download")
        partial.delete()

        val modelUrl = URL(
            "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/" +
                "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm?download=true"
        )
        val connection = modelUrl.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 180_000
        connection.setRequestProperty("User-Agent", "COPY-Android-Build/0.2")

        try {
            val status = connection.responseCode
            check(status in 200..299) {
                "Unable to download the bundled COPY model (HTTP $status). " +
                    "Check access to huggingface.co and retry the Android build."
            }
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            check(partial.length() > 250_000_000L) {
                "The downloaded local model is unexpectedly small; refusing to package an invalid file."
            }
            check(partial.renameTo(target)) {
                "Downloaded the local model, but could not move it into the Android assets directory."
            }
            logger.lifecycle("COPY local model bundled: ${target.length()} bytes")
        } finally {
            connection.disconnect()
            partial.delete()
        }
    }
}

// The weights are obtained from the public Apache-2.0 LiteRT Community model
// during the build, not committed to Git. The generated APK contains the weights.
tasks.named("preBuild").configure {
    dependsOn(downloadBundledLocalAiModel)
}
