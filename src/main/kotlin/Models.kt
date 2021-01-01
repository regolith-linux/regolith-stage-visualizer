import java.net.URI

typealias PPAVersionIndex = Map<PPADescriptor, Map<UbuntuRelease, PPAPackageInfo>>
typealias PackageIndex = Map<PackageInfo, PPAVersionIndex>

typealias MutablePPAVersionIndex = MutableMap<PPADescriptor, MutableMap<UbuntuRelease, PPAPackageInfo>>
typealias MutablePackageIndex = MutableMap<PackageInfo, MutablePPAVersionIndex>

data class CrossPPAPackageDescriptor(
    val packageInfo: PackageInfo,
    val versionMap: PPAVersionIndex
)

data class PPAPackageInfo(val version: String, val changeLog: String?, val buildStatus: String)

sealed class PPADescriptor(val baseUrl: String, val name: String) {
    fun generateUrl(): URI =
        URI.create("$baseUrl+packages?batch=275&start=1")

    fun generateSuiteUri(): String = "~regolith-linux/ubuntu/${name.toLowerCase()}"

    companion object {
        fun fromName(name: String): PPADescriptor = when(name.toUpperCase()) {
            "COPYTEST" -> COPYTEST
            "EXPERIMENTAL" -> EXPERIMENTAL
            "UNSTABLE" -> UNSTABLE
            "STABLE" -> STABLE
            "RELEASE" -> RELEASE
            "REGOLITH-1.4.1" -> REGOLITH_141
            else -> error("Unkwown PPA name $name")
        }
    }

    override fun toString(): String {
        return name
    }
}

object COPYTEST : PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/copytest/", "COPYTEST")
object EXPERIMENTAL : PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/experimental/", "EXPERIMENTAL")
object UNSTABLE : PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/unstable/", "UNSTABLE")
object STABLE: PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/stable/", "STABLE")
object RELEASE: PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/release/", "RELEASE")
object REGOLITH_141: PPADescriptor("https://launchpad.net/~regolith-linux/+archive/ubuntu/regolith-1.4.1/", "REGOLITH-1.4.1")
class CustomLaunchpadDescriptor(url: String, name: String): PPADescriptor(url, name)

enum class UbuntuRelease(val active: Boolean = true) {
    bionic, eoan(false), focal, groovy
}

data class PackageInfo(val name: String, val description: String?) {
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

data class PackageDescriptor(val info: PackageInfo, val ppa: PPADescriptor, val status: String, val changeLog: String?, val version: String, val release: UbuntuRelease)