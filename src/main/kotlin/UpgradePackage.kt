/**
 * Generate a shell script to upgrade packages from one PPA to another.
 */

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

    println(CommandCollector.collect())
}

fun upgradePackageVersions(
    pkgInfo: PackageInfo,
    sourceVersions: Pair<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>,
    targetVersions: Pair<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>
) {
    val packagesUpgraded = mutableSetOf<PackageInfo>()

    sourceVersions.second.forEach { (release, pkgPPAInfo) ->
        if (targetVersions.second[release] != pkgPPAInfo && !packagesUpgraded.contains(pkgInfo)) {
            if (uniformVersions(sourceVersions.second)) {
                upgradePackageUniformVersions(
                    pkgInfo,
                    sourceVersions.second.keys,
                    sourceVersions.first,
                    targetVersions.first,
                    pkgPPAInfo.version,
                    targetVersions.second[release]?.version,
                    targetVersions.second
                )
            } else {
                upgradePackage(
                    pkgInfo,
                    sourceVersions,
                    targetVersions,
                    pkgPPAInfo.version,
                    targetVersions.second[release]?.version
                )
            }
            packagesUpgraded.add(pkgInfo)
        }
    }
}

// Determine if the set of versions are uniform (all are the same).
private fun uniformVersions(versions: Map<UbuntuRelease, PPAPackageInfo>): Boolean =
    versions.entries.map { it.value }.toSet().size == 1

// copy-package --from=$SOURCE_PPA --from-suite=$SOURCE_VERSION --to=$TARGET_PPA --to-suite=$TARGET_VERSION -b -y $PACKAGE
private fun upgradePackage(
    pkgInfo: PackageInfo,
    sourceVersions: Pair<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>,
    targetVersions: Pair<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>,
    targetVersion: String,
    oldVersion: String?
) {
    val sourcePPA = sourceVersions.first
    val targetPPA = targetVersions.first

    sourceVersions.second.map { it.key }.sorted().forEach { ubuntuRelease ->
        if (targetVersions.second[ubuntuRelease]?.version != targetVersion) { // Verify target version is not already same as source version
            CommandCollector.addCommand(
                pkgInfo.name, """
            # Upgrading non-uniform $pkgInfo ($sourcePPA -> $targetPPA) from $oldVersion to $targetVersion
            copy-package --from=${sourcePPA.generateSuiteUri()} --from-suite=${ubuntuRelease.name} --to=${targetPPA.generateSuiteUri()} --to-suite=${ubuntuRelease.name} -b -y ${pkgInfo.name}
        """.trimIndent()
            )
        }
    }
}

private fun upgradePackageUniformVersions(
    pkgInfo: PackageInfo,
    versionSet: Set<UbuntuRelease>,
    sourcePPA: PPADescriptor,
    targetPPA: PPADescriptor,
    targetVersion: String,
    oldVersion: String?,
    targetVersions: Map<UbuntuRelease, PPAPackageInfo>
) {
    // Copy earliest version from source to target PPA
    val earliestVersion = versionSet.min()!!
    val samePPACopyVersions = (versionSet - earliestVersion).sorted()

    if (targetVersions[earliestVersion]?.version != targetVersion) { // Verify target version is not already same as source version
        CommandCollector.addCommand(
            pkgInfo.name, """
        # Upgrading $pkgInfo ($sourcePPA -> $targetPPA) from $oldVersion to $targetVersion.
        copy-package --from=${sourcePPA.generateSuiteUri()} --from-suite=${earliestVersion.name} --to=${targetPPA.generateSuiteUri()} --to-suite=${earliestVersion.name} -b -y ${pkgInfo.name}
    """.trimIndent()
        )
    }

    // Once copied, use target copy to other ubuntu versions in target PPA
    samePPACopyVersions.forEach { ubuntuSuite ->
        if (targetVersions[ubuntuSuite]?.version != targetVersion) { // Verify target version is not already same as source version
            CommandCollector.addCommand(
                pkgInfo.name, """
            # Upgrading uniform $pkgInfo from $oldVersion to $targetVersion in $targetPPA
            copy-package --from=${targetPPA.generateSuiteUri()} --from-suite=${earliestVersion.name} --to=${targetPPA.generateSuiteUri()} --to-suite=${ubuntuSuite.name} -b -y ${pkgInfo.name}
        """.trimIndent()
            )
        }
    }
}
