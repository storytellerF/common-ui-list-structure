import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.system.exitProcess

const val extractGradleVersionRegExp =
    "\\d+\\.\\d+(\\.\\d+)?(\\-[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*)?(\\+[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*)?"

fun main() {

    val commonUiListAgpMatcher =
        commonVersionMatcher(extractGradleVersionRegExp, "../common-ui-list/build.gradle.kts")
    val versionManagerAgpMatcher = versionManagerMatcher(extractGradleVersionRegExp)

    val giantExplorerAgpMatcher =
        commonVersionMatcher(extractGradleVersionRegExp, "../examples/GiantExplorer/build.gradle.kts")
    val pingAgpMatcher = commonVersionMatcher(extractGradleVersionRegExp, "../examples/Ping/build.gradle.kts")
    val yueAgpMatcher = yueAgpMatcher("../giant-explorer/yue/build.gradle")
    val liAgpMatcher = yueAgpMatcher("../giant-explorer/li/build.gradle")

    val matchers = listOf(
        commonUiListAgpMatcher,
        versionManagerAgpMatcher,
        giantExplorerAgpMatcher,
        pingAgpMatcher,
        yueAgpMatcher,
        liAgpMatcher
    )
    if (matchers.all {
            it.find()
        }) {
        val versions = matchers.map {
            it.group(1)
        }
        if (versions.distinct().size > 1) {
            System.err.println("agp版本不一致 ${versions.joinToString()}")
            exitProcess(1)
        }
    } else {
        System.err.println("出错了，请检查agp 版本")
        exitProcess(1)
    }
}

private fun yueAgpMatcher(filePath: String): Matcher {
    val readText = File(filePath).readText()
    val compile = Pattern.compile("id 'com.android.application' version '($extractGradleVersionRegExp)' apply false")
    return compile
        .matcher(readText)
}

private fun commonVersionMatcher(extractGradleVersionRegExp: String, filePath: String): Matcher {
    val commonUiListGradleText = File(filePath).readText()
    return Pattern.compile("val androidVersion = \"($extractGradleVersionRegExp)\"").matcher(commonUiListGradleText)
}

private fun versionManagerMatcher(extractGradleVersionRegExp: String): Matcher {
    val versionManagerGradleText = File("../common-ui-list/version-manager/build.gradle.kts").readText()
    return Pattern.compile("com.android.tools.build:gradle:($extractGradleVersionRegExp)")
        .matcher(versionManagerGradleText)
}