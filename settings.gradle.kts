pluginManagement {
    repositories {
//        maven { setUrl("https://jitpack.io") }
//        maven { setUrl("https://maven.aliyun.com/repository/central") }
//        maven { setUrl("https://maven.aliyun.com/repository/google") }
//        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { setUrl("https://maven.aliyun.com/repository/public") }
//        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}


dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    versionCatalogs {
        create("compose") { from(files("gradle/compose.versions.toml")) }
        create("app") { from(files("gradle/app.versions.toml") )}
    }
    repositories {
        mavenLocal()
//        maven { setUrl("https://plugins.gradle.org/m2/") }
//        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
//        maven { setUrl("https://jitpack.io") }
//        maven { setUrl("https://maven.aliyun.com/repository/central") }
//        maven { setUrl("https://maven.aliyun.com/repository/google") }
//        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { setUrl("https://maven.aliyun.com/repository/public") }
//        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "AttrTextLayout"
include(":app")
include(":textview")
