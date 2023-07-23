import java.io.File
import java.util.regex.Pattern
import kotlin.system.exitProcess

fun main() {
    val regex = "\\d+\\.\\d+(\\.\\d+)?(\\-[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*)?(\\+[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*)?"
    val commonUiListGradleText = File("../common-ui-list/build.gradle.kts").readText()
    val versionManagerGradleText = File("../common-ui-list/version-manager/build.gradle.kts").readText()
    val commonUiListAgpMatcher = Pattern.compile("val androidVersion = \"($regex)\"").matcher(commonUiListGradleText)
    val versionManagerAgpMatcher = Pattern.compile("com.android.tools.build:gradle:($regex)").matcher(versionManagerGradleText)
    if (commonUiListAgpMatcher.find() && versionManagerAgpMatcher.find()) {
        val commonUiListAgpVersion = commonUiListAgpMatcher.group(1)
        val versionManagerAgpVersion = versionManagerAgpMatcher.group(1)
        if (commonUiListAgpVersion != versionManagerAgpVersion) {
            System.err.println("agp版本不一致 common-ui-list $commonUiListAgpVersion versionManager $versionManagerAgpVersion")
            exitProcess(1)
        }
    } else {
        System.err.println("出错了，请检查agp 版本")
        exitProcess(1)
    }
}