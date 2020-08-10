/**
 * Launchpad mutations must adhere to specific timing constraints; Package mutations can not occur in parallel within suites (Ubuntu versions).
 *
 * This class attempts to deal with this by batching operations per-package and waiting between batches in hopes that build
 * operations will complete.
 */

object CommandCollector {
    data class Command(val packageName: String, val scriptBlock: String)

    private val SLEEP_COMMAND = Command("_", "sleep 1200")

    private val buffer: MutableList<MutableList<Command>> = mutableListOf()
    private val dedupeBuffer = mutableSetOf<String>()

    fun addCommand(packageName: String, command: String) {
        if (dedupeBuffer.contains(command)) return // Ignore duplicate commands

        val cmd = Command(packageName, command)

        val emptyCommandList = buffer.firstOrNull { !it.containsPackage(packageName) }

        if (emptyCommandList != null) {
            emptyCommandList.add(cmd)
        } else {
            buffer.add(mutableListOf(cmd))
        }

        dedupeBuffer.add(command)
    }

    fun collect(nextOnly: Boolean = false): String {
        val source = if (nextOnly) listOf(buffer.first()) else buffer

        return source
            .map {
                it.add(SLEEP_COMMAND)
                it
            }
            .flatten()
            .map { command ->
                command.scriptBlock
            }
            .joinToString(separator = "\n") { it }
    }

    private fun Iterable<Command>.containsPackage(packageName: String): Boolean =
        this.any { cmd -> cmd.packageName == packageName }
}