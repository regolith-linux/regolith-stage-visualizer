fun main(args: Array<String>) {
    val sourcePPA: PPADescriptor = PPADescriptor.fromName(args.getOrElse(0) { "UNSTABLE" })
    val targetPPA: PPADescriptor = PPADescriptor.fromName(args.getOrElse(1) { "STABLE" })
    val packageNameFilter: String? = args.getOrNull(2)

    val index = fetchPackageIndex(sourcePPA, targetPPA, fetchExtras = false, pkgNameMatch = packageNameFilter)

    index
        .filter { it.value.containsKey(sourcePPA) } // only consider packages in source PPA
        .forEach { pkgEntry ->
            val sourceVersions = pkgEntry.value[sourcePPA] ?: error("Expected value")
            val targetVersions = pkgEntry.value[targetPPA] ?: mapOf()

            if (sourceVersions != targetVersions)
                upgradePackageVersions(pkgEntry.key, sourcePPA to sourceVersions, targetPPA to targetVersions)
        }
}

fun upgradePackageVersions(
    pkgInfo: PackageInfo,
    sourceVersions: Pair<PPADescriptor, Map<UbuntuRelease, PackageVersion>>,
    targetVersions: Pair<PPADescriptor, Map<UbuntuRelease, PackageVersion>>
) {
    val packagesUpgraded = mutableSetOf<PackageInfo>()

    sourceVersions.second.forEach { (release, version) ->
        if (targetVersions.second[release] != version && !packagesUpgraded.contains(pkgInfo)) {
            upgradePackageUniformVersions(pkgInfo, sourceVersions.first, targetVersions.first, version, targetVersions.second[release])
            packagesUpgraded.add(pkgInfo)
        }
    }
}

fun upgradePackageUniformVersions(
    pkgInfo: PackageInfo,
    sourcePPA: PPADescriptor,
    targetPPA: PPADescriptor,
    targetVersion: PackageVersion,
    oldVersion: PackageVersion?
) {
    println("# echo Upgrading $pkgInfo from $oldVersion to $targetVersion.")
    println("./promote-stage.sh ${sourcePPA.name.toLowerCase()} ${targetPPA.name.toLowerCase()} $pkgInfo")
}
