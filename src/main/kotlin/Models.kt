import java.net.URI

//typealias CrossPPAPackageDescriptor = Map<>

data class CrossPPAPackageDescriptor(
    val packageName: PackageName,
    val versionMap: Map<PPADescriptor, Map<UbuntuRelease, PackageVersion>>
)

enum class PPADescriptor {
    UNSTABLE {
        override fun generateUrl(): URI =
            URI.create("https://launchpad.net/~regolith-linux/+archive/ubuntu/unstable/+packages?batch=275&start=1")
    },
    STABLE {
        override fun generateUrl(): URI =
            URI.create("https://launchpad.net/~regolith-linux/+archive/ubuntu/stable/+packages?batch=275&start=1")
    },
    RELEASE {
        override fun generateUrl(): URI =
            URI.create("https://launchpad.net/~regolith-linux/+archive/ubuntu/release/+packages?batch=275&start=1")
    };

    abstract fun generateUrl(): URI
}

enum class UbuntuRelease {
    BIONIC, EOAN, FOCAL
}

data class PackageVersion(val rawVersion: String)

data class PackageName(val name: String)

data class PackageDescriptor(val name: PackageName, val ppa: PPADescriptor, val version: PackageVersion, val release: UbuntuRelease)