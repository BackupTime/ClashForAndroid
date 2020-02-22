import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import java.util.zip.ZipFile

open class GolangBindTask : DefaultTask() {
    companion object {
        private val STUB_GO_MOD_CONTENT = """
            module github.com/kr328/cfa-bind
            
            require (
                github.com/kr328/cfa v0.0.0 // redirect
            )
            
            replace github.com/kr328/cfa v0.0.0 => {SOURCE_PATH}
        """.trimIndent()
        private val STUB_GO_FILE_CONTENT = """
            package main
            
            import "github.com/kr328/cfa/bridge"
            
            func main() {}
        """.trimIndent()
        private val REGEX_REPLACE_TARGET_LOCAL = Regex("=>\\s+\\./")
        private val REGEX_REPLACE_SOURCE_VERSION = Regex("v.+\\s+=>")
    }

    private val javaOutput: File
        get() {
            return project.buildDir.resolve("intermediates/go_output/generate_java")
        }
    private val nativeOutput: File
        get() {
            return project.buildDir.resolve("intermediates/go_output/native_library")
        }
    private val goBuildPath: File
        get() {
            return project.buildDir.resolve("intermediates/go_build")
        }
    private val goPath: File
        get() {
            return goBuildPath.resolve("go_path")
        }
    private val goBindPath: File
        get() {
            return goBuildPath.resolve("go_bind_path")
        }
    private val sourcePath: File
        get() {
            return project.file("src/main/golang")
        }
    private val properties by lazy {
        FileReader(project.rootProject.file("local.properties")).use {
            Properties().apply { load(it) }
        }
    }
    private val environment = mutableMapOf<String, String>()

    init {
        onlyIf {
            val lastModify = sourcePath.walk()
                .filter { it.extension == "go" || it.extension == "mod" }
                .map { it.lastModified() }
                .max() ?: 0L

            return@onlyIf goBuildPath.resolve("bridge.aar").lastModified() < lastModify
        }
    }

    @TaskAction
    fun process() {
        environment["GOPATH"] = goPath.absolutePath
        environment["ANDROID_HOME"] = findAndroidSdkPath().absolutePath
        environment["ANDROID_NDK_HOME"] = findAndroidNdkPath().absolutePath

        if (Os.isFamily(Os.FAMILY_WINDOWS))
            environment["Path"] = System.getenv("Path") + ";" + goPath.resolve("bin")
        else
            environment["PATH"] = System.getenv("PATH") + ":" + goPath.resolve("bin")

        goPath.resolve("src/github.com/kr328").deleteRecursively()
        goBindPath.deleteRecursively()
        goBindPath.mkdirs()

        "go get golang.org/x/mobile/cmd/gomobile".exec()

        FileWriter(goBindPath.resolve("go.mod")).use {
            it.write(buildStubGoModule(sourcePath))
        }
        FileWriter(goBindPath.resolve("main.go")).use {
            it.write(STUB_GO_FILE_CONTENT)
        }

        "go mod vendor".exec(goBindPath)

        goBindPath.resolve("vendor")
            .copyRecursively(goPath.resolve("src"), overwrite = true)

        "gomobile init".exec(goBuildPath)
        "gomobile bind -target=android \"-gcflags=all=-trimpath=$goPath\" github.com/kr328/cfa/bridge".exec(goBuildPath)

        nativeOutput.deleteRecursively()
        javaOutput.deleteRecursively()

        with(ZipFile(goBuildPath.resolve("bridge.aar"))) {
            stream()
                .filter { !it.isDirectory }
                .filter { it.name.startsWith("jni") }
                .forEach {
                    val target = nativeOutput.resolve(it.name.removePrefix("jni/"))

                    target.parentFile.mkdirs()

                    FileOutputStream(target).use { output ->
                        getInputStream(it).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
        }

        with(ZipFile(goBuildPath.resolve("bridge-sources.jar"))) {
            stream()
                .filter { !it.isDirectory }
                .filter { it.name.endsWith(".java") }
                .forEach {
                    val target = javaOutput.resolve(it.name)

                    target.parentFile.mkdirs()

                    FileOutputStream(target).use { output ->
                        getInputStream(it).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
        }
    }

    private fun findAndroidNdkPath(): File {
        return properties.getProperty("ndk.dir")?.let { File(it) }?.takeIf { it.exists() }
            ?: throw GradleException("Android NDK not found.")
    }

    private fun findAndroidSdkPath(): File {
        return properties.getProperty("sdk.dir")?.let { File(it) }?.takeIf { it.exists() }
            ?: throw GradleException("Android SDK not found.")
    }

    private fun buildStubGoModule(source: File): String {
        val replaces = source.walk()
            .filter { it.name == "go.mod" }
            .flatMap { file ->
                file.readLines()
                    .asSequence()
                    .filter { line -> line.startsWith("replace") }
                    .map { replace ->
                        replace.replace(REGEX_REPLACE_TARGET_LOCAL, "=> " + file.parentFile.absolutePath.replace('\\','/') + "/")
                    }
                    .map { replace ->
                        replace.replace(REGEX_REPLACE_SOURCE_VERSION, " =>")
                    }
            }
            .joinToString("\n")

        return STUB_GO_MOD_CONTENT.replace("{SOURCE_PATH}", source.absolutePath) +
                "\n\n" + replaces
    }

    private fun String.exec(pwd: File = File(".")) {
        val process = with(ProcessBuilder()) {
            if (Os.isFamily(Os.FAMILY_WINDOWS))
                command("cmd.exe", "/c", this@exec)
            else
                command("bash", "-c", this@exec)

            environment().putAll(environment)
            directory(pwd)

            redirectErrorStream(true)

            start()
        }

        process.inputStream.copyTo(System.out)
        System.out.flush()

        if (process.waitFor() != 0)
            throw GradleException("Run command $this failure")
    }
}