import java.util.regex.Pattern

fun main() {
    val userHome = System.getProperty("user.home")
    val adbPath = "$userHome/Library/Android/sdk/platform-tools/adb"
    val modulePath = "."
    val fileName = "yue-html.zip"
    val target = "$modulePath/build/$fileName"
    val output = "/data/data/com.storyteller_f.giant_explorer/files/plugins/$fileName"
    val tmp = "/data/local/tmp/$fileName"
    val getDevicesCommand = Runtime.getRuntime().exec(arrayOf(adbPath, "devices"))
    getDevicesCommand.waitFor()
    val readText = getDevicesCommand.inputStream.bufferedReader().use {
        it.readText().trim()
    }
    getDevicesCommand.destroy()
    val devices = readText.split("\n").let {
        it.subList(1, it.size)
    }.map {
        it.split(Pattern.compile("\\s+")).first()
    }
    command(arrayOf("zip", target, "$modulePath/index.html", "$modulePath/imgTouchCanvas.js"))
    devices.forEach {
        println("dispatch to $it")
        command(arrayOf(adbPath, "-s", it, "push", target, tmp))
        command(arrayOf(adbPath, "-s", it, "shell", "run-as", "com.storyteller_f.giant_explorer", "sh", "-c", "\'cp $tmp $output\'"))
        command(arrayOf(adbPath, "-s", it, "shell", "sh", "-c", "\'rm $tmp\'"))
    }
}


fun command(arrayOf: Array<String>): Int {
    val pushCommand = Runtime.getRuntime().exec(arrayOf)
    val waitFor = pushCommand.waitFor()
    pushCommand.destroy()
    return waitFor
}