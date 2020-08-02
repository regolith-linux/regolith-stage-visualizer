import java.lang.IllegalStateException

fun main(args: Array<String>) {
    val sourcePPA: PPADescriptor = UNSTABLE
    val targetPPA: PPADescriptor = STABLE

    val index = fetchPackageIndex(sourcePPA, targetPPA, fetchExtras = false)

    index
        .filter { it.value.containsKey(sourcePPA) } // only consider packages in source PPA
        .forEach { pkgEntry ->
            // println("Evaluating ${pkgEntry.key}")

            val sourceVersions = pkgEntry.value[sourcePPA] ?: throw IllegalStateException("Expected value")
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
            upgradePackage(pkgInfo, sourceVersions.first, targetVersions.first, version, targetVersions.second[release])
            packagesUpgraded.add(pkgInfo)
        }
    }
}

fun upgradePackage(
    pkgInfo: PackageInfo,
    sourcePPA: PPADescriptor,
    targetPPA: PPADescriptor,
    targetVersion: PackageVersion,
    oldVersion: PackageVersion?
) {
    println("# echo Upgrading $pkgInfo from $oldVersion ($targetPPA) to $targetVersion ($sourcePPA).")
    println("./promote-stage.sh ${sourcePPA.name.toLowerCase()} ${targetPPA.name.toLowerCase()} $pkgInfo")
}
