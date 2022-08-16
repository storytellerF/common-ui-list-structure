import common_ui_list_structure_preset.*

plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-parcelize")
    id ("androidx.navigation.safeargs.kotlin")
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
        buildConfigField( "String",
                "File_PROVIDER_AUTHORITY",
                "\"${fileProvider}\"")
    }
}

dependencies {
    implementation (fileTree("libs"))
    fileSystemDependency()
    networkDependency()
    workerDependency()
    implementation ("androidx.preference:preference:1.2.0")
    implementation ("com.j256.simplemagic:simplemagic:1.17")
    implementation ("com.github.osama-raddad:FireCrasher:2.0.0")

}
baseApp()
setupGeneric()
setupDataBinding()