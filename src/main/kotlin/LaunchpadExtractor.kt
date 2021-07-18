import org.jsoup.Jsoup
import java.lang.IllegalArgumentException

private data class IntermediatePackageInfo(val nameAndVersion: String, val buildStatus: String, val extrasLink: String, val ubuntuRelease: UbuntuRelease)

/**
 * Produce a list of PackageDescriptor for packages from the specified URI
 * @param source definition of source URI
 */
fun readPPA(source: PPADescriptor, loadExtras: Boolean = true, pkgNameMatch: String? = null): List<PackageDescriptor> =
    fetchPackageInfo(source)
        .map { pkgInfo ->
            val nvTokens = pkgInfo.nameAndVersion.split(" - ")
            require(nvTokens.size == 2) { "Unexpected string parse for $nvTokens."}

            // If a filter is set, match name against package and return null if no match.
            if (pkgNameMatch != null && !nvTokens[0].contains(pkgNameMatch)) {
                return@map null
            }

            val (changelog, desc) = if (loadExtras)
                readPackageExtras(pkgInfo.extrasLink)
            else
                null to null

            PackageDescriptor(PackageInfo(nvTokens[0], desc), source, pkgInfo.buildStatus, changelog, nvTokens[1], pkgInfo.ubuntuRelease)
        }
        .filterNotNull()
        .toList()

fun readPackageExtras(extrasLink: String): Pair<String, String> {
        val doc = Jsoup.connect(extrasLink).get()

        val changeLog = doc.getElementsByClass("changelog").first().text()
        val description =
            doc.getElementsContainingOwnText("Built packages").first().nextElementSibling().getElementsByTag("li")
                .first().html()
        val htmlTermPos = description.indexOf("</b>") + "</b>".length

        return changeLog to description.substring(htmlTermPos).trim()
}

fun fetchPackageIndex(vararg ppas: PPADescriptor, fetchExtras: Boolean = false, pkgNameMatch: String? = null): PackageIndex {
    val packageIndex: MutablePackageIndex = mutableMapOf()

    ppas
        .map { uri -> readPPA(uri, fetchExtras, pkgNameMatch) }
        .apply { collect(this, packageIndex) }

    return packageIndex
}

fun collect(packages: List<List<PackageDescriptor>>, container: MutablePackageIndex) {
    packages
        .flatten()
        .forEach { packageDescriptor ->
            if (!container.containsKey(packageDescriptor.info)) container[packageDescriptor.info] = mutableMapOf()
            if (!container[packageDescriptor.info]!!.containsKey(packageDescriptor.ppa)) container[packageDescriptor.info]!![packageDescriptor.ppa] = mutableMapOf()

            container[packageDescriptor.info]!![packageDescriptor.ppa]!![packageDescriptor.release] = PPAPackageInfo(packageDescriptor.version, packageDescriptor.changeLog, packageDescriptor.status)
        }
}

private fun fetchPackageInfo(source: PPADescriptor): List<IntermediatePackageInfo> {
    val doc = Jsoup.connect(source.generateUrl().toURL().toString()).get()

    return doc.select("tr.archive_package_row").mapNotNull { element ->
        val nameAndVersion = element.getElementsByTag("a").firstOrNull()?.text() ?: return@mapNotNull null
        val status  = element.getElementsByTag("td").getOrNull(3)?.text() ?: return@mapNotNull null
        val release = element.getElementsByTag("td").getOrNull(4)?.text() ?: return@mapNotNull null
        val extrasLink = element.getElementsByTag("a").firstOrNull()?.attributes()?.get("href") ?: return@mapNotNull null

        val releaseEnum = try {
            UbuntuRelease.valueOf(release.toLowerCase())
        } catch (e: IllegalArgumentException) {
            error("Unknown Ubuntu version: $release")
        }

        IntermediatePackageInfo(nameAndVersion, status, source.baseUrl + extrasLink, releaseEnum)
    }
}