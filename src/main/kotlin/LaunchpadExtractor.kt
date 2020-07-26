import org.jsoup.Jsoup
import java.lang.Exception

private data class IntermediatePackageInfo(val nameAndVersion: String, val extrasLink: String, val ubuntuRelease: UbuntuRelease)

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
            val extrasLink = element.getElementsByTag("a").firstOrNull()?.attributes()?.get("href") ?: return@mapNotNull null

            IntermediatePackageInfo(nameAndVersion, source.baseUrl + extrasLink, UbuntuRelease.valueOf(release.toUpperCase()))
        }
        .map { pkgInfo ->
            val nvTokens = pkgInfo.nameAndVersion.split(" - ")
            require(nvTokens.size == 2) { "Unexpected string parse for $nvTokens."}

            val (changelog, desc) = readPackageExtras(pkgInfo.extrasLink)

            PackageDescriptor(PackageInfo(nvTokens[0], changelog, desc), source, PackageVersion(nvTokens[1]), pkgInfo.ubuntuRelease)
        }
        .toList()
}

fun readPackageExtras(extrasLink: String): Pair<String, String> {
        val doc = Jsoup.connect(extrasLink).get()

        val changeLog = doc.getElementsByClass("changelog").first().text()
        val description =
            doc.getElementsContainingOwnText("Built packages").first().nextElementSibling().getElementsByTag("li")
                .first().html()
        val htmlTermPos = description.indexOf("</b>") + "</b>".length

        return changeLog to description.substring(htmlTermPos).trim()
}
