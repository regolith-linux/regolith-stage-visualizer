/**
 * List packages in a PPA.
 */
fun main(args: Array<String>) {
    val ppaDescriptor = PPADescriptor.fromName(args.getOrElse(0) { "RELEASE" })

    printPackages(readPPA(ppaDescriptor, false))
}

fun printPackages(pkgList: Iterable<PackageDescriptor>) {
    pkgList.forEach { pkgDesc ->
        println("${pkgDesc.info.name}\t${pkgDesc.release.name}\t${pkgDesc.version}\t${pkgDesc.status}")
    }
}