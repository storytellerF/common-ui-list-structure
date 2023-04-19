import com.storyteller_f.version_manager.*

val filterFolder: String by project

class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> {
        // Note: If you're using KSP, you should change the line below to return
        // listOf("room.schemaLocation=${schemaDir.path}")
        return listOf("-Aroom.schemaLocation=${schemaDir.path}")
    }
}

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
    buildFeatures {
        viewBinding = true
    }

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
        javaCompileOptions {
            annotationProcessorOptions {
                compilerArgumentProviders(
                    RoomSchemaArgProvider(File(projectDir, "schemas"))
                )
            }
        }
    }
    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    implementation(fileTree("libs"))
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    fileSystemDependency()
    networkDependency()
    workerDependency()
    implementation("androidx.preference:preference:1.2.0")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    implementation("androidx.browser:browser:1.5.0")

    val libsuVersion = "5.0.3"

    // The core module that provides APIs to a shell
    implementation("com.github.topjohnwu.libsu:core:${libsuVersion}")

    // Optional: APIs for creating root services. Depends on ":core"
    implementation("com.github.topjohnwu.libsu:service:${libsuVersion}")

    // Optional: Provides remote file system support
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")

    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation(project(":app:giant-explorer-plugin-core"))
    implementation("androidx.webkit:webkit:1.6.1")

    //filter & sort
    val home: String = System.getProperty("user.home")
    val debugFilterFolder = file("$home/AndroidStudioProjects/FilterUIProject/")
    val subModuleFilterFolder = file("./FilterUIProject")
    val currentFolder = when (filterFolder) {
        "local" -> debugFilterFolder
        "submodule" -> subModuleFilterFolder
        else -> null
    }
    if (currentFolder?.exists() == true) {
        implementation(project(":filter:config-core"))
        implementation(project(":filter:sort-core"))
        implementation(project(":filter:filter-core"))
        implementation(project(":filter:filter-ui"))
        implementation(project(":filter:sort-ui"))
    } else {
        implementation("com.github.storytellerF:FilterUIProject:1.0")
    }
    // https://mvnrepository.com/artifact/commons-net/commons-net
    implementation("commons-net:commons-net:3.9.0")
    // https://mvnrepository.com/artifact/org.mockftpserver/MockFtpServer
    testImplementation("org.mockftpserver:MockFtpServer:3.1.0")
    implementation(project(":compat-ktx"))
    androidTestImplementation("androidx.room:room-testing:2.5.1")

    // https://mvnrepository.com/artifact/com.hierynomus/smbj
    implementation("com.hierynomus:smbj:0.11.5")
    // https://mvnrepository.com/artifact/com.hierynomus/sshj
    implementation("com.hierynomus:sshj:0.35.0")
    implementation("com.madgag.spongycastle:core:1.58.0.0")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")
}
baseApp()
setupGeneric()
setupDataBinding()