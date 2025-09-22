plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "cn.huolala.threadshield"
    compileSdk = 34

    packaging {
        jniLibs.excludes.add("**/libshadowhook.so")
    }

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++11")
            }
        }
        ndk {
            abiFilters.apply {
                add("armeabi-v7a")
                add("arm64-v8a")
            }
        }
    }
    buildFeatures {
        prefab = true
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
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.shadowhook)
}

//publishing {
//    publications {
//        create<MavenPublication>("release") {
//            groupId = "com.github.HuolalaTech"
//            artifactId = "hll-sys-hook-android"
//            version = "1.4-SNAPSHOT"
//
//            afterEvaluate {
//                val pubComponent = components.findByName("release")
//                if (pubComponent != null) {
//                    from(pubComponent)
//                }
//            }
//        }
//    }
//
//    repositories {
//        maven {
//            name = "jitpack"
//            url = uri("https://jitpack.io")
//        }
//    }
//}


mavenPublishing {

    publishToMavenCentral(true)
    signAllPublications()

    coordinates("io.github.korilin", "hll-sys-hook-android","1.4-KORILIN-V15FIX-3")

    pom {
        name = "HuoLaLa unify hook lib"
        description = "Fix some system bugs to enhance app stability."
        url = "https://github.com/korilin/hll-sys-hook-android"
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://github.com/korilin/hll-sys-hook-android/blob/main/LICENSE.txt"
            }
        }
        scm {
            url = "https://github.com/korilin/hll-sys-hook-android"
            connection = "scm:git:git://github.com/korilin/hll-sys-hook-android.git"
            developerConnection = "scm:git:ssh://git@github.com/korilin/hll-sys-hook-android.git"
        }
        developers {
            developer {
                id = "HuolalaTech"
                name = "HuolalaTech"
                email = ""
                url = "https://github.com/HuolalaTech"
            }

            developer {
                id = "korilin"
                name = "Kori"
                email = "korilin.dev@gmail.com"
                url = "https://github.com/korilin"
            }
        }
    }
}


tasks.named("publishToMavenLocal") {
    dependsOn("assembleRelease")
}