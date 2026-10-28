plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.serloss.fcl.driver.radvxclipse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.serloss.fcl.driver.radvxclipse"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets["main"].jniLibs.srcDir("src/main/jniLibs")
}

val radvLib = file("src/main/jniLibs/arm64-v8a/vulkan.radeon.so")

tasks.register("checkRadvDriver") {
    doFirst {
        if (!radvLib.isFile) {
            throw GradleException("Missing ${radvLib.path}. Run ../scripts/fetch-radv.sh (or scripts/fetch-radv.sh) before building.")
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("checkRadvDriver")
}
