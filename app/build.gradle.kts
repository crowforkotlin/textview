plugins {
    alias(app.plugins.android.application)
    alias(app.plugins.android.kotlin)
}

android {
    namespace = "com.crow.attrtextlayout"
    compileSdk = 34
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        applicationId = "com.crow.attrtextlayout"
        minSdk = 21
        targetSdk = 34
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":crow-attr-text"))
//    implementation(libs.crow.attrTextLayout)
//    implementation(files("/Users/crowforkotlin/AndroidStudioProjects/AttrTextLayout/crow-attr-text/component/AttrTextLayout-1.0.jar"))
    implementation(app.androidx.core.ktx)
    implementation(app.androidx.appcompat)
    implementation(app.androidx.material)
    implementation(app.androidx.activity)
    implementation(app.androidx.constraintlayout)
}