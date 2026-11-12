import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException
import java.net.URL
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File

abstract class DownloadTask : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()

        var tempFile: File? = null

        try {
            tempFile = File.createTempFile("download", ".tmp", out.parentFile)
            var currentUrl = url.get()
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                val urlObj = URL(currentUrl)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = false

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location == null) {
                        throw GradleException("Redirect missing Location header")
                    }
                    currentUrl = location
                    connection.disconnect()
                    redirectCount++
                } else if (responseCode in 200..299) {
                    break
                } else {
                    throw GradleException("HTTP error $responseCode while downloading")
                }
            }

            if (connection == null) {
                throw GradleException("Failed to download after $redirectCount redirects")
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            if (tempFile.length() == 0L) {
                throw GradleException("Downloaded file is empty")
            }

            Files.move(
                tempFile.toPath(),
                out.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            if (!out.exists() || out.length() == 0L) {
                throw GradleException("Failed to download and no cached version exists: ${e.message}", e)
            } else {
                logger.warn("Failed to update photo, using cached version: ${e.message}")
            }
        } finally {
            tempFile?.delete()
        }
    }
}
