import org.apache.tools.ant.taskdefs.condition.Os
import java.io.*
import java.util.*
import java.net.*
import java.time.*

val gMinSdkVersion: Int by rootProject.extra

val geoipDatabaseUrl = "https://github.com/Dreamacro/maxmind-geoip/releases/latest/download/Country.mmdb"
val geoipInvalidate = Duration.ofDays(7)
val geoipOutput = buildDir.resolve("outputs/geoip")
val golangSource = file("src/main/golang")
val golangOutput = buildDir.resolve("outputs/golang")
val nativeAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

fun generateGolangBuildEnvironment(abi: String): Map<String, String> {
    val properties = Properties().apply {
        load(FileInputStream(rootProject.file("local.properties")))
    }

    val ndk = properties.getProperty("ndk.dir")
        ?: throw GradleScriptException("ndk.dir not found in local.properties",
            FileNotFoundException("ndk.dir not found in local.properties"))

    val host = when {
        Os.isFamily(Os.FAMILY_WINDOWS) ->
            "windows"
        Os.isFamily(Os.FAMILY_MAC) ->
            "darwin"
        Os.isFamily(Os.FAMILY_UNIX) ->
            "linux"
        else ->
            throw GradleScriptException("Unsupported host", FileNotFoundException("Unsupported host"))
    }

    val compilerBase = rootProject.file(ndk).resolve("toolchains/llvm/prebuilt/$host-x86_64/bin")

    val cCompiler = when(abi) {
        "armeabi-v7a" ->
            "armv7a-$host-androideabi$gMinSdkVersion-clang"
        "arm64-v8a" ->
            "aarch64-$host-android$gMinSdkVersion-clang"
        "x86" ->
            "i686-$host-android$gMinSdkVersion-clang"
        "x86_64" ->
            "x86_64-$host-android$gMinSdkVersion-clang"
        else ->
            throw GradleScriptException("Unsupported abi $abi", FileNotFoundException("Unsupported abi $abi"))
    }

    val cppCompiler = when(abi) {
        "armeabi-v7a" ->
            "armv7a-$host-androideabi$gMinSdkVersion-clang++"
        "arm64-v8a" ->
            "aarch64-$host-android$gMinSdkVersion-clang++"
        "x86" ->
            "i686-$host-android$gMinSdkVersion-clang++"
        "x86_64" ->
            "x86_64-$host-android$gMinSdkVersion-clang++"
        else ->
            throw GradleScriptException("Unsupported abi $abi", FileNotFoundException("Unsupported abi $abi"))
    }

    val linker =  when(abi) {
        "armeabi-v7a" ->
            "arm-$host-androideabi-ld"
        "arm64-v8a" ->
            "aarch64-$host-android-ld"
        "x86" ->
            "i686-$host-android-ld"
        "x86_64" ->
            "x86_64-$host-android-ld"
        else ->
            throw GradleScriptException("Unsupported abi $abi", FileNotFoundException("Unsupported abi $abi"))
    }

    val golangArch = when(abi) {
        "armeabi-v7a" ->
            "arm"
        "arm64-v8a" ->
            "arm64"
        "x86" ->
            "386"
        "x86_64" ->
            "amd64"
        else ->
            throw GradleScriptException("Unsupported abi $abi", FileNotFoundException("Unsupported abi $abi"))
    }

    return mapOf(
        "CC" to compilerBase.resolve(cCompiler).absolutePath,
        "CXX" to compilerBase.resolve(cppCompiler).absolutePath,
        "LD" to compilerBase.resolve(linker).absolutePath,
        "GOOS" to "android",
        "GOARCH" to golangArch,
        "CGO_ENABLED" to "1",
        "CFLAGS" to "-O3 -Werror"
    )
}

fun String.exec(pwd: File = buildDir, env: Map<String, String> = System.getenv()): String {
    val process = ProcessBuilder().run {
        if ( Os.isFamily(Os.FAMILY_WINDOWS) )
            command("cmd.exe", "/c", this@exec)
        else
            command("bash", "-c", this@exec)

        environment().putAll(env)
        directory(pwd)

        redirectErrorStream(true)

        start()
    }

    val outputStream = ByteArrayOutputStream()
    process.inputStream.copyTo(outputStream)

    if ( process.waitFor() != 0 ) {
        println(outputStream.toString("utf-8"))
        throw GradleScriptException("Exec $this failure", IOException())
    }

    return outputStream.toString("utf-8")
}

task("compileClashCore") {
    onlyIf {
        val sourceModified = golangSource.walk()
            .filter { it.extension == "go" || it.extension == "c" || it.extension == "h" || it.name == "go.mod" }
            .map { it.lastModified() }
            .max() ?: Long.MAX_VALUE
        val targetModified = golangOutput.walk()
            .filter { it.extension == "so" }
            .map { it.lastModified() }
            .min() ?: Long.MIN_VALUE

        sourceModified > targetModified
    }

    doLast {
        nativeAbis.parallelStream().forEach {
            val env = generateGolangBuildEnvironment(it)
            val out = golangOutput.resolve(it).apply {
                mkdirs()
            }.resolve("libclash.so")

            "go build --buildmode=c-shared -o \"$out\"".exec(pwd = golangSource, env = env)
        }
    }
}

task("downloadGeoipDatabase") {
    val geoipFile = geoipOutput.resolve("Country.mmdb")

    onlyIf {
        System.currentTimeMillis() - geoipFile.lastModified() > geoipInvalidate.toMillis()
    }

    doLast {
        geoipOutput.mkdirs()

        URL(geoipDatabaseUrl).openConnection().getInputStream().use { input ->
            FileOutputStream(geoipFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}
