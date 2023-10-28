import com.android.build.api.dsl.ApplicationDefaultConfig
import com.storyteller_f.version_manager.*

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
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
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
        registerConfigKey("file-provider")
        registerConfigKey("file-system-provider")
        registerConfigKey("file-system-encrypted-provider")
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

    implementation("androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINTLAYOUT}")
    implementation("androidx.navigation:navigation-fragment-ktx:${Versions.NAVIGATION}")
    implementation("androidx.navigation:navigation-ui-ktx:${Versions.NAVIGATION}")
    fileSystemDependency()
    networkDependency()
    workerDependency()
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    implementation("androidx.browser:browser:1.6.0")

    val libsuVersion = "5.0.3"

    // The core module that provides APIs to a shell
    implementation("com.github.topjohnwu.libsu:core:${libsuVersion}")

    // Optional: APIs for creating root services. Depends on ":core"
    implementation("com.github.topjohnwu.libsu:service:${libsuVersion}")

    // Optional: Provides remote file system support
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation(project(":giant-explorer-plugin-core"))
    implementation("androidx.webkit:webkit:1.8.0")
    androidTestImplementation("androidx.room:room-testing:2.6.0")
    //filter & sort
    val filterArtifact = listOf("config-core", "sort-core", "filter-core", "filter-ui", "sort-ui")

    val filterModules = filterArtifact.mapNotNull {
        findProject(":filter:$it")
    }
    if (filterModules.size == filterArtifact.size) {
        filterModules.forEach {
            implementation(it)
        }
    } else {
        filterArtifact.forEach {
            implementation("com.github.storytellerF.Shun:$it:1.0.0")
        }
    }
    implementation("com.madgag.spongycastle:core:1.58.0.0")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")

    implementation("androidx.window:window:1.2.0-beta04")
    val liPluginModule = findProject(":li-plugin")
    if (liPluginModule != null) {
        implementation(liPluginModule)
    }
}
constraintCommonUIListVersion("cbf40f5ff3")
fileSystemDependency()
baseApp()
setupGeneric()
setupDataBinding()
setupPreviewFeature()

fun ApplicationDefaultConfig.registerConfigKey(
    identification: String,
) {
    val placeholderKey = placeholderKey(identification)
    val buildConfigKey = buildConfigKey(placeholderKey)
    val fileSystemProviderEncryptedAuthority = "$applicationId.$identification"

    // Now we can use ${documentsAuthority} in our Manifest
    manifestPlaceholders[placeholderKey] = fileSystemProviderEncryptedAuthority
    // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
    buildConfigField(
        "String",
        buildConfigKey.toString(),
        "\"${fileSystemProviderEncryptedAuthority}\""
    )
}

fun buildConfigKey(placeholderKey: String): StringBuilder {
    val configKey = StringBuilder()
    placeholderKey.forEachIndexed { index, c ->
        configKey.append(
            when {
                index == 0 -> c.uppercase()
                c.isUpperCase() && placeholderKey[index - 1].isLowerCase() -> "_$c"
                else -> c.uppercase()
            }
        )
    }
    return configKey
}

fun placeholderKey(identification: String): String {
    val identifyString = StringBuilder()
    var i = 0
    while (i < identification.length) {
        val c = identification[i++]
        if (c == '-') {
            identifyString.append(identification[i++].uppercase())
        } else identifyString.append(c)
    }
    return identifyString.append("Authority").toString()
}
