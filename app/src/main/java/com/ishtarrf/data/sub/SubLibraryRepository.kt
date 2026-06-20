package com.ishtarrf.data.sub

import android.content.Context
import com.ishtarrf.domain.SubEntry
import java.io.File

/**
 * A flat-with-folders library of `.sub` files stored under the app's private
 * `files/signals/` directory. The root directory itself is the "default" folder;
 * each immediate subdirectory is a named folder.
 */
class SubLibraryRepository(context: Context) {

    val root: File = File(context.filesDir, "signals").apply { mkdirs() }

    fun folders(): List<String> {
        val subs = root.listFiles { f -> f.isDirectory }
            ?.map { it.name }
            ?.filter { it != DEFAULT }
            ?.sorted()
            ?: emptyList()
        return listOf(DEFAULT) + subs
    }

    private fun dirFor(folder: String): File =
        if (folder == DEFAULT) root else File(root, sanitize(folder)).apply { mkdirs() }

    fun list(folder: String): List<SubEntry> =
        dirFor(folder).listFiles { f -> f.isFile && f.extension == "sub" }
            ?.sortedBy { it.name.lowercase() }
            ?.map { SubEntry(it.nameWithoutExtension, folder, it.absolutePath) }
            ?: emptyList()

    fun listAll(): List<SubEntry> = folders().flatMap { list(it) }

    fun save(folder: String, name: String, content: String): SubEntry {
        val safe = sanitize(name)
        val file = File(dirFor(folder), "$safe.sub")
        file.writeText(content)
        return SubEntry(safe, folder, file.absolutePath)
    }

    fun read(entry: SubEntry): String = File(entry.path).readText()

    fun delete(entry: SubEntry): Boolean = File(entry.path).delete()

    fun rename(entry: SubEntry, newName: String): SubEntry {
        val safe = sanitize(newName)
        val source = File(entry.path)
        val target = File(source.parentFile, "$safe.sub")
        source.renameTo(target)
        return SubEntry(safe, entry.folder, target.absolutePath)
    }

    fun createFolder(name: String) {
        val safe = sanitize(name)
        if (safe.isNotEmpty()) File(root, safe).mkdirs()
    }

    fun fileFor(entry: SubEntry): File = File(entry.path)

    private fun sanitize(name: String): String =
        name.trim().filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifEmpty { "signal" }

    companion object {
        const val DEFAULT = "default"
    }
}
