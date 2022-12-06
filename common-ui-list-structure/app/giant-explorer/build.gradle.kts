import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {

    defaultConfig {
        applicationId = "com.storyteller_f.giant_explorer"
    }

    namespace = "com.storyteller_f.giant_explorer"

    defaultConfig {
        val fileProvider = "$applicationId.file-provider"

        // Now we can use ${documentsAuthority} in our Manifest
        manifestPlaceholders["providerAuthority"] = fileProvider
        // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
        buildConfigField(
            "String",
            "FILE_PROVIDER_AUTHORITY",
            "\"${fileProvider}\""
        )
        val fileSystemProvider = "$applicationId.file-system-provider"

        // Now we can use ${documentsAuthority} in our Manifest
        manifestPlaceholders["fileSystemProviderAuthority"] = fileSystemProvider
        // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
        buildConfigField(
            "String",
            "FILE_SYSTEM_PROVIDER_AUTHORITY",
            "\"${fileSystemProvider}\""
        )
    }
}

dependencies {
    implementation(fileTree("libs"))
    fileSystemDependency()
    networkDependency()
    workerDependency()
    implementation("androidx.preference:preference:1.2.0")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    implementation("com.github.osama-raddad:FireCrasher:2.0.0")

    val libsuVersion = "5.0.3"

    // The core module that provides APIs to a shell
    implementation("com.github.topjohnwu.libsu:core:${libsuVersion}")

    // Optional: APIs for creating root services. Depends on ":core"
    implementation("com.github.topjohnwu.libsu:service:${libsuVersion}")

    // Optional: Provides remote file system support
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")
}
baseApp()
setupGeneric()
setupDataBinding()