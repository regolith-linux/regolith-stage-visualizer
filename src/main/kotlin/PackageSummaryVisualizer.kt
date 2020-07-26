fun main() {
    /*
    val items = PPADescriptor.values()
        .map { uri -> readPPA(uri) }
        .flatten()
        .map { desc -> desc.name to mapOf(desc.release to desc.version) }
        .toMap()
*/
    val ts = mutableMapOf<PackageName, MutableMap<PPADescriptor, MutableMap<UbuntuRelease, PackageVersion>>>()

    PPADescriptor.values()
        .map { uri -> readPPA(uri) }
        .flatten()
        .forEach { packageDescriptor ->
            if (!ts.containsKey(packageDescriptor.name)) ts[packageDescriptor.name] = mutableMapOf()
            if (!ts[packageDescriptor.name]!!.containsKey(packageDescriptor.ppa)) ts[packageDescriptor.name]!![packageDescriptor.ppa] = mutableMapOf()


            ts[packageDescriptor.name]!![packageDescriptor.ppa]!![packageDescriptor.release] = packageDescriptor.version
        }

    println(generateHtml(ts))
}