import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.application"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.application"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Pleos Playground 내 프로젝트의 SDK 클라이언트 ID/시크릿 (TTS/STT HYBRID 모드용). local.properties에 넣으면 주입됨
        val props = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField("String", "PLEOS_CLIENT_ID", "\"${props.getProperty("pleos.clientId", "")}\"")
        buildConfigField("String", "PLEOS_CLIENT_SECRET", "\"${props.getProperty("pleos.clientSecret", "")}\"")
        // 백엔드가 준비되기 전까지는 기기 내 Fake 데이터 사용
        buildConfigField("String", "API_BASE_URL", "\"${props.getProperty("api.baseUrl", "")}\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)
    implementation(libs.gson)

    // Pleos Connect SDK
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.tts)
    implementation(libs.pleos.stt)
    implementation(libs.pleos.fusedlocation)
    // Pleos SDK는 Kotlin으로 작성돼 있어 Java에서 콜백(Function0/Function1)을 쓰려면 필요
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}