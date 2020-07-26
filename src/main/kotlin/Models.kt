import java.net.URI

//typealias CrossPPAPackageDescriptor = Map<>

data class CrossPPAPackageDescriptor(
    val packageInfo: PackageInfo,
    val versionMap: Map<PPADescriptor, Map<UbuntuRelease, PackageVersion>>
)

enum class PPADescriptor(val baseUrl: String) {
    UNSTABLE("https://launchpad.net/~regolith-linux/+archive/ubuntu/unstable/"),
    STABLE("https://launchpad.net/~regolith-linux/+archive/ubuntu/stable/"),
    RELEASE("https://launchpad.net/~regolith-linux/+archive/ubuntu/release/");

    fun generateUrl(): URI =
        URI.create("$baseUrl+packages?batch=275&start=1")
}

enum class UbuntuRelease {
    BIONIC, EOAN, FOCAL
}

data class PackageVersion(val rawVersion: String)

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
}

data class PackageDescriptor(val info: PackageInfo, val ppa: PPADescriptor, val version: PackageVersion, val release: UbuntuRelease)