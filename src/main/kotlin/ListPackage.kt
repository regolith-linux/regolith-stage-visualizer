/**
 * List packages in a PPA.
 */
fun main(args: Array<String>) {
    val ppaURL = args.getOrNull(0) ?: throw IllegalArgumentException("Usage: lsppa <PPA URL | UNSTABLE | STABLE | RELEASE> [ppa name]")
    val ppaName = args.getOrElse(1) { "unspecified" }

    val ppaDescriptor = when (ppaURL) {
        "UNSTABLE" -> UNSTABLE
        "STABLE" -> STABLE
        "RELEASE" -> RELEASE
        else -> CustomLaunchpadDescriptor(ppaURL, ppaName)
    }

    printPackages(readPPA(ppaDescriptor, false))
}

fun printPackages(pkgList: Iterable<PackageDescriptor>) {
    pkgList.forEach { pkgDesc ->
        println("${pkgDesc.info.name}\t${pkgDesc.release.name}\t${pkgDesc.version.rawVersion}")
    }
}