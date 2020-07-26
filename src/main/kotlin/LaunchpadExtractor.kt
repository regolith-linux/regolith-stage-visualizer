import org.jsoup.Jsoup

/**
 * Produce a list of PackageDescriptor for packages from the specified URI
 * @param source definition of source URI
 */
fun readPPA(source: PPADescriptor): List<PackageDescriptor> {
    val doc = Jsoup.connect(source.generateUrl().toURL().toString()).get()

    return doc.select("tr.archive_package_row")
        .mapNotNull { element ->
            val nameAndVersion = element.getElementsByTag("a").firstOrNull()?.text() ?: return@mapNotNull null
            val release = element.getElementsByTag("td").getOrNull(4)?.text() ?: return@mapNotNull null

            nameAndVersion to UbuntuRelease.valueOf(release.toUpperCase())
        }
        .map { (nameAndVersion, release) ->
            val nvTokens = nameAndVersion.split(" - ")
            require(nvTokens.size == 2) { "Unexpected string parse for $nvTokens."}

            PackageDescriptor(PackageName(nvTokens[0]), source, PackageVersion(nvTokens[1]), release)
        }
        .toList()
}