package io.github.genesjc.opensourcepanda.data

import android.util.AtomicFile
import android.util.Log
import com.squareup.moshi.JsonAdapter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.concurrent.Executors

private const val TAG = "JsonListFile"

/**
 * One list of objects saved as JSON in a file in private storage: the job shared by the bookmarks and the
 * hidden issues. Writes go through Android's [AtomicFile]: the old content is kept as a backup until the new
 * content is fully written, and the backup is restored if the write fails or the app is killed part way. So a
 * read sees the old list or the new one, never half of one. Writes run in order on one background thread, so
 * two quick changes cannot land out of order. [adapterFactory] builds the Moshi adapter on first use.
 */
internal class JsonListFile<T>(
    file: File,
    adapterFactory: () -> JsonAdapter<List<T>>,
) {
    private val atomicFile = AtomicFile(file)
    private val adapter by lazy(adapterFactory)
    private val writer = Executors.newSingleThreadExecutor()

    /**
     * The saved list, or an empty list when nothing is saved yet. A file that is there but cannot be read as a
     * list (a format this version does not know) also reads as empty, is logged, and is replaced by the next
     * save; a future change to the saved format needs a migration.
     */
    fun load(): List<T> = try {
        adapter.fromJson(atomicFile.readFully().toString(Charsets.UTF_8)) ?: emptyList()
    } catch (e: FileNotFoundException) {
        emptyList()
    } catch (e: Exception) {
        Log.w(TAG, "The saved list could not be read; starting empty", e)
        emptyList()
    }

    /**
     * Saves [items] in the background. A failed write leaves the previous content in place, is logged, and calls
     * [onFailure] (on the writer thread) so the owner can tell the user.
     */
    fun save(items: List<T>, onFailure: () -> Unit) {
        writer.execute {
            var out: FileOutputStream? = null
            try {
                // The new content goes to a fresh file and replaces the old one only at finishWrite(), so a
                // crash anywhere before that leaves the previous list intact.
                out = atomicFile.startWrite()
                out.write(adapter.toJson(items).toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(out)
            } catch (e: Exception) {
                out?.let(atomicFile::failWrite)
                Log.w(TAG, "The list could not be saved", e)
                onFailure()
            }
        }
    }
}
