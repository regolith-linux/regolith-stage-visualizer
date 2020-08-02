/**
 * Generate HTML page of packages across Regolith PPAs.
 */
fun main() {
    println(
        generateHtml(
            fetchPackageIndex(
                UNSTABLE,
                STABLE,
                RELEASE,
                fetchExtras = true
            )))
}