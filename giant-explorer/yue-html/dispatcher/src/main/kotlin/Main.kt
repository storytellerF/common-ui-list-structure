import java.io.File

fun main(args: Array<String>) {
    println("Hello World!")
    val userHome = System.getProperty("user.home")
    val adbPath = "$userHome/Library/Android/sdk/platform-tools/adb"
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
    val path = args.firstOrNull() ?: "../"
    println(File(path).absolutePath)
    extracted(
        listOf(File(path, "build/yue-html.zip")),
        listOf("com.storyteller_f.giant_explorer" to "files/plugins"),
        listOf(),
        adbPath,
        "yue-html.zip"
    )
}