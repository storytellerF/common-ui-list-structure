import java.io.File
import java.util.regex.Pattern

fun extracted(
    transferFiles: List<File>,
    packageTargets: List<Pair<String, String>>,
    pathTargets: List<String>,
    adbPath: String,
    outputName: String
) {
    val tmp = "/data/local/tmp/${outputName}"
    val devices = getDevices(adbPath)
    transferFiles.forEach {
        val src = it.absolutePath
        devices.forEach { deviceSerial ->
            println("dispatch to $deviceSerial")
            pathTargets.forEach {
                command(arrayOf(adbPath, "-s", deviceSerial, "push", src, it))
            }
            command(arrayOf(adbPath, "-s", deviceSerial, "push", src, tmp))
            packageTargets.forEach { (pn, sp) ->
                val outputPath = "/data/data/$pn/$sp"
                val output = "$outputPath/${outputName}"
                command(createOutputCommand(deviceSerial, pn, outputPath, adbPath))
                command(createCopyCommand(adbPath, deviceSerial, pn, tmp, output))
            }
            command(arrayOf(adbPath, "-s", deviceSerial, "shell", "sh", "-c", "\'rm $tmp\'"))
        }
    }

}

private fun getDevices(adbPath: String): List<String> {
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
    return devices
}

private fun createCopyCommand(
    adbPath: String,
    deviceSerial: String,
    pn: String,
    tmp: String,
    output: String
): Array<String> {
    return arrayOf(
        adbPath,
        "-s",
        deviceSerial,
        "shell",
        "run-as",
        pn,
        "sh",
        "-c",
        "\'cp $tmp $output\'"
    )
}

private fun createOutputCommand(
    deviceSerial: String,
    pn: String,
    outputPath: String,
    adbPath: String
): Array<String> {
    return arrayOf(
        adbPath,
        "-s",
        deviceSerial,
        "shell",
        "run-as",
        pn,
        "sh",
        "-c",
        "\'mkdir $outputPath\'"
    )
}

private fun command(arrayOf: Array<String>): Int {
    val pushCommand = Runtime.getRuntime().exec(arrayOf)
    val waitFor = pushCommand.waitFor()
    pushCommand.destroy()
    return waitFor
}