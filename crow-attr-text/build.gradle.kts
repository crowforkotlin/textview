import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(app.plugins.android.library)
    alias(app.plugins.android.kotlin)
    alias(libs.plugins.mavenPublish)
}

android {
    namespace = "com.crow.text"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        setProperty("archivesBaseName", "AttrTextLayout-${properties["VERSION_NAME"]}")
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
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            res.srcDir("src/main/res")
            assets.srcDirs("src/main/assets")
            kotlin.srcDirs("src/main/kotlin")
        }
    }
}

dependencies {
//    implementation(app.androidx.core.ktx)
//    implementation(app.androidx.appcompat)
//    implementation(app.androidx.material)
//    implementation(app.androidx.activity)
//    implementation(app.androidx.constraintlayout)
//    implementation(libs.kotlin.coroutines.core)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
    configure(AndroidMultiVariantLibrary(
        sourcesJar = false,
        publishJavadocJar = false,
        includedBuildTypeValues = setOf("release")
    ))
}

afterEvaluate {
    tasks.getByName("createFullJarRelease") {
//        android.sourceSets.getByName("main").apply { from(assets.srcDirs, java.srcDirs, res.srcDirs, "${projectDir.absolutePath}/src/main/kotlin") }
//        archiveClassifier.set("")
//        archiveBaseName.set("${properties["POM_ARTIFACT_ID"]}")
//        archiveVersion.set("${properties["VERSION_NAME"]}")
        doLast {
            copy {
                from("${projectDir.absolutePath}/build/intermediates/full_jar/release/createFullJarRelease/full.jar")
                into("${projectDir.absolutePath}/component")
                rename {
                    "${properties["POM_ARTIFACT_ID"]}-${properties["VERSION_NAME"]}.jar"
                }
            }
        }
    }
    tasks.getByName("assembleRelease") {
        doLast {
            copy {
                println("${projectDir.absolutePath}/build/outputs/aar/${properties["archivesBaseName"]}-release.aar")
                from("${projectDir.absolutePath}/build/outputs/aar/${properties["archivesBaseName"]}-release.aar")
                into("${projectDir.absolutePath}/component")
                rename { "${properties["archivesBaseName"]}.aar" }
            }
        }
    }
    tasks.getByName("buildComponent") {
        dependsOn("createFullJarRelease", "assembleRelease")
    }
}

tasks.register("buildComponent")