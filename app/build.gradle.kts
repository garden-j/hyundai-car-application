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
        // 상세 버전: 앱이 카카오·기상청을 직접 호출할 때 쓰는 키(저장소에 넣지 않음)
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"${props.getProperty("kakao.restApiKey", "")}\"")
        buildConfigField("String", "KMA_SERVICE_KEY", "\"${props.getProperty("kma.serviceKey", "")}\"")
        // (선택) 별도 백엔드 서버 주소. 위 카카오 키가 있으면 서버 대신 앱이 직접 호출한다
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
    testOptions {
        // 단위 테스트에서 android.util.Log 등 호출 시 예외 대신 기본값 반환
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    testImplementation(libs.assertj)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}