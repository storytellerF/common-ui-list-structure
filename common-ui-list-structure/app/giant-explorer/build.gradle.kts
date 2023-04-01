import com.storyteller_f.version_manager.*
val filterDebug: String by project

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.storyteller_f.version_manager")
}

android {

    defaultConfig {
        applicationId = "com.storyteller_f.giant_explorer"
    }

    namespace = "com.storyteller_f.giant_explorer"

    defaultConfig {
        val fileProviderAuthority = "$applicationId.file-provider"

        // Now we can use ${documentsAuthority} in our Manifest
        manifestPlaceholders["providerAuthority"] = fileProviderAuthority
        // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
        buildConfigField(
            "String",
            "FILE_PROVIDER_AUTHORITY",
            "\"${fileProviderAuthority}\""
        )
        val fileSystemProviderAuthority = "$applicationId.file-system-provider"

        // Now we can use ${documentsAuthority} in our Manifest
        manifestPlaceholders["fileSystemProviderAuthority"] = fileSystemProviderAuthority
        // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
        buildConfigField(
            "String",
            "FILE_SYSTEM_PROVIDER_AUTHORITY",
            "\"${fileSystemProviderAuthority}\""
        )

        val fileSystemProviderEncryptedAuthority = "$applicationId.file-system-encrypted-provider"

        // Now we can use ${documentsAuthority} in our Manifest
        manifestPlaceholders["fileSystemProviderEncryptedAuthority"] = fileSystemProviderEncryptedAuthority
        // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
        buildConfigField(
            "String",
            "FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY",
            "\"${fileSystemProviderEncryptedAuthority}\""
        )
    }
}

dependencies {
    implementation(fileTree("libs"))
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}")
    fileSystemDependency()
    networkDependency()
    workerDependency()
    implementation("androidx.preference:preference:1.2.0")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    implementation("com.github.osama-raddad:FireCrasher:2.0.0")
    implementation("androidx.browser:browser:1.5.0")

    val libsuVersion = "5.0.3"

    // The core module that provides APIs to a shell
    implementation("com.github.topjohnwu.libsu:core:${libsuVersion}")

    // Optional: APIs for creating root services. Depends on ":core"
    implementation("com.github.topjohnwu.libsu:service:${libsuVersion}")

    // Optional: Provides remote file system support
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")

    implementation("com.github.bumptech.glide:glide:4.15.0")

    implementation(project(":app:giant-explorer-plugin-core"))
    implementation("androidx.webkit:webkit:1.6.1")

    //filter & sort
    if (filterDebug.toBoolean()) {
        implementation(project(":filter:config-core"))
        implementation(project(":filter:sort-core"))
        implementation(project(":filter:filter-core"))
        implementation(project(":filter:filter-ui"))
        implementation(project(":filter:sort-ui"))
    } else {
        implementation("com.github.storytellerF:FilterUIProject:1.0")
    }

}
baseApp()
setupGeneric()
setupDataBinding()