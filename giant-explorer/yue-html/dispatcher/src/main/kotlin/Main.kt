import com.storyteller_f.song.SongAction
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("dispatcher")
    val userHome = System.getProperty("user.home")
    val adbPath = "$userHome/Library/Android/sdk/platform-tools/adb"
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    logger.info("Program arguments: ${args.joinToString()}")
    val path = args.firstOrNull() ?: "../"
    logger.debug(File(path).absolutePath)
    SongAction(
        listOf(File(path, "build/yue-html.zip")),
        listOf("com.storyteller_f.giant_explorer" to "files/plugins"),
        listOf(),
        adbPath,
        "yue-html.zip",
        logger
    ).dispatchToMultiDevices()
}