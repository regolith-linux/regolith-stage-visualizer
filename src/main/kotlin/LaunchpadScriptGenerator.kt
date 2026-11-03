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

    // The script for the next batch (nextOnly) or for all batches, each
    // followed by a sleep. Reading it leaves the batches as they are, and an
    // empty collector gives an empty script.
    fun collect(nextOnly: Boolean = false): String {
        val source = if (nextOnly) buffer.take(1) else buffer

        return source
            .flatMap { batch -> batch + SLEEP_COMMAND }
            .joinToString(separator = "\n") { command -> command.scriptBlock }
    }

    private fun Iterable<Command>.containsPackage(packageName: String): Boolean =
        this.any { cmd -> cmd.packageName == packageName }
}