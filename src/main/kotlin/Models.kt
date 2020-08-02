import java.net.URI

typealias PPAVersionIndex = Map<PPADescriptor, Map<UbuntuRelease, PackageVersion>>
typealias PackageIndex = Map<PackageInfo, PPAVersionIndex>

typealias MutablePPAVersionIndex = MutableMap<PPADescriptor, MutableMap<UbuntuRelease, PackageVersion>>
typealias MutablePackageIndex = MutableMap<PackageInfo, MutablePPAVersionIndex>

data class CrossPPAPackageDescriptor(
    val packageInfo: PackageInfo,
    val versionMap: PPAVersionIndex
)

sealed class PPADescriptor(val baseUrl: String, val name: String) {
    fun generateUrl(): URI =
        URI.create("$baseUrl+packages?batch=275&start=1")
}

object UNSTABLE : PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/unstable/", "UNSTABLE")
object STABLE: PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/stable/", "STABLE")
object RELEASE: PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/release/", "RELEASE")
class CustomLaunchpadDescriptor(url: String, name: String): PPADescriptor(url, name)

enum class UbuntuRelease {
    BIONIC, EOAN, FOCAL
}

data class PackageVersion(val rawVersion: String) {
    override fun toString(): String {
        return rawVersion
    }
}

data class PackageInfo(val name: String, val changeLog: String, val description: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackageInfo

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return name
    }
}

data class PackageDescriptor(val info: PackageInfo, val ppa: PPADescriptor, val version: PackageVersion, val release: UbuntuRelease)