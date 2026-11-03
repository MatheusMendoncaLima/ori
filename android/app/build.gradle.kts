plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

// The schema of the device database is written to schemas/ at each version, so a new migration can be
// checked against the real old schema instead of SQL typed by hand (see docs/dados.md).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.oriteam.ori"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oriteam.ori"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // One flavor per environment. Each has its own Firebase configuration in
    // src/<flavor>/google-services.json, so dev data never mixes with real users.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // Local Firebase Emulator Suite. 10.0.2.2 is the host computer seen from the Android emulator;
            // on a physical device use `adb reverse` and -PoriEmulatorHost=127.0.0.1.
            val emulatorHost = (project.findProperty("oriEmulatorHost") as String?) ?: "10.0.2.2"
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$emulatorHost\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"\"")
        }
        create("prod") {
            dimension = "environment"
            // Default in Android Studio, so "Run" keeps using the real project as before.
            isDefault = true
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"\"")
        }
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric needs merged resources to render Compose UI tests on the JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    // Staging needs its own Firebase project. Until src/staging/google-services.json exists,
    // its variants are disabled instead of failing every build.
    beforeVariants { variant ->
        if (variant.flavorName == "staging" && !file("src/staging/google-services.json").exists()) {
            variant.enable = false
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
    // `./gradlew detekt -PdetektAutoCorrect` applies the ktlint formatting fixes.
    autoCorrect = project.hasProperty("detektAutoCorrect")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.googlefonts)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    detektPlugins(libs.detekt.formatting)
}
