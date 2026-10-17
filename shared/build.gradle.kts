plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    id("kotlin-parcelize")
}

kotlin {
    androidLibrary {
       namespace = "org.dazai.osmium.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       androidResources.enable = true
    }
    
    jvm()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.icons)
            implementation(libs.compose.icons.extended)
            implementation(libs.mpfilepicker)
            implementation(libs.coil.compose)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.webview)
        }
        androidMain.dependencies {
            implementation(libs.readium.shared)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.appcompat)
            implementation(libs.readium.streamer)
            implementation(libs.readium.navigator)
            implementation(libs.androidx.viewpager2)
            implementation(libs.tinypinyin)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.tinypinyin)
            implementation("org.json:json:20240303")
            implementation(libs.kcef)
        }
        
        // 更通用的方式来定义 iOS 源代码集
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }
        
        targets.all {
            if (name.startsWith("ios")) {
                compilations.getByName("main").defaultSourceSet.dependsOn(iosMain)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

sqldelight {
    databases {
        create("OsmiumDatabase") {
            packageName = "org.dazai.osmium.db"
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
