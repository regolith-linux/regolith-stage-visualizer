fun main() {
    /*
    val items = PPADescriptor.values()
        .map { uri -> readPPA(uri) }
        .flatten()
        .map { desc -> desc.name to mapOf(desc.release to desc.version) }
        .toMap()
*/
    val ts = mutableMapOf<PackageInfo, MutableMap<PPADescriptor, MutableMap<UbuntuRelease, PackageVersion>>>()

    PPADescriptor.values()
        .map { uri -> readPPA(uri) }
        .flatten()
        .forEach { packageDescriptor ->
            if (!ts.containsKey(packageDescriptor.info)) ts[packageDescriptor.info] = mutableMapOf()
            if (!ts[packageDescriptor.info]!!.containsKey(packageDescriptor.ppa)) ts[packageDescriptor.info]!![packageDescriptor.ppa] = mutableMapOf()


            ts[packageDescriptor.info]!![packageDescriptor.ppa]!![packageDescriptor.release] = packageDescriptor.version
        }

    println(generateHtml(ts))
}