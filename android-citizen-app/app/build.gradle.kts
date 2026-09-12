plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sih.trial2"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sih.trial2"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.maplibre.sdk)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("io.socket:socket.io-client:2.1.1") {
        // socket.io-client ships an older org.json; Android provides one.
        exclude(group = "org.json", module = "json")
    }
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}