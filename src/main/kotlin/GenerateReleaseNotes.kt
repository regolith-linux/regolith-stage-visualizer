import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.kgilmer.identist.httpGet
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths


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
                generateReleaseNotes(pkgEntry.key, sourcePPA to sourceVersions, targetPPA to targetVersions)
        }
}

fun generateReleaseNotes(
    pkgInfo: PackageInfo,
    sourceVersions: Pair<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>,
    targetVersions: Pair<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>
) {
    val packagesUpgraded = mutableSetOf<PackageInfo>()
    val earliestVersion = sourceVersions.second.keys.min() ?: error("Unable to get min version.")

    sourceVersions.second.forEach { (release, pkgPPAInfo) ->
        if (targetVersions.second[release] != pkgPPAInfo && !packagesUpgraded.contains(pkgInfo)) {
            // https://launchpad.net/~regolith-linux/+archive/ubuntu/stable/+sourcefiles/regolith-rofi-config/1.2.6-1/regolith-rofi-config_1.2.6-1.debian.tar.xz
            val pkgName = pkgInfo.name
            val pkgVersion = (sourceVersions.second[earliestVersion] ?: error("Can't find version for $earliestVersion")).version
            val debianTarballUrl = "${sourceVersions.first.baseUrl}+sourcefiles/${pkgName}/${pkgVersion}/${pkgName}_$pkgVersion.debian.tar.xz"
            val completeTarballUrl = "${sourceVersions.first.baseUrl}+sourcefiles/${pkgName}/${pkgVersion}/${pkgName}_$pkgVersion.tar.xz"
            // println("wget $debianTarballUrl")
            val changelog = downloadAndExtractChangelog(pkgName, listOf(debianTarballUrl, completeTarballUrl))
            val subChangelog = bisectChangelog(changelog, targetVersions.second[earliestVersion]?.version, (sourceVersions.second[earliestVersion] ?: error("Expected version matrix")).version)

            println("#".repeat(40))
            println("# Release Notes for $pkgName")
            println("#".repeat(40))
            println(subChangelog)

            if (uniformVersions(sourceVersions.second)) {
                // println("from: ${(sourceVersions.second[earliestVersion] ?: error("Failed")).changeLog}")
                // println("to: ${(targetVersions.second[earliestVersion] ?: error("Failed")).changeLog}")
            } else {

            }
            packagesUpgraded.add(pkgInfo)
        }
    }
}

fun bisectChangelog(changelog: List<String>, lowerVersion: String?, laterVersion: String): String {
    val buffer = StringBuilder()
    var copyFlag = false

    for (line in changelog) {
        if (line.contains("($laterVersion)")) copyFlag = true
        if (lowerVersion != null && line.contains("$lowerVersion")) copyFlag = false
        if (copyFlag && !line.startsWith(" -- ")) buffer.append(line).append("\n")
    }

    return buffer.toString()
}

fun downloadAndExtractChangelog(packageName: String, debianTarballUrl: List<String>): List<String> {
    val filename = "/tmp/$packageName.tar.xz"
    val outputFile = BufferedOutputStream(FileOutputStream(filename))
    for (downloadUrl in debianTarballUrl) {
        val success = URL(downloadUrl).httpGet { statusCode, _, body ->
            // println("Attempting $downloadUrl")
            if (statusCode >= 300) return@httpGet false
            val bytesWritten = body?.copyTo(outputFile) ?: return@httpGet false
            if (bytesWritten < 1) return@httpGet false
            outputFile.flush()
            outputFile.close()
            // println("Saved $filename")
            return@httpGet true
        }

        if (success) {
            Files.newInputStream(Paths.get(filename)).use { fi ->
                BufferedInputStream(fi).use { bi ->
                    XZCompressorInputStream(bi).use { gzi ->
                        TarArchiveInputStream(gzi).use { o ->
                            var entry: ArchiveEntry?
                            do {
                                entry = o.nextEntry

                                if (entry != null) {
                                    if (!o.canReadEntryData(entry)) continue
                                    if (entry.isDirectory) continue
                                    if (!entry.name.endsWith("debian/changelog")) continue

                                    val changelogFilename = "/tmp/changelog-$packageName"
                                    val changelogOS = BufferedOutputStream(FileOutputStream(changelogFilename))

                                    o.copyTo(changelogOS)
                                    changelogOS.flush()
                                    changelogOS.close()

                                    // println("Wrote $changelogFilename")

                                    return File(changelogFilename).readLines()
                                }
                            } while(entry != null)
                        }
                    }
                }
            }
        }
    }

    return emptyList()
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
