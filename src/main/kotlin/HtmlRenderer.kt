import java.time.LocalDateTime
import kotlin.math.absoluteValue

fun generateHtml(packageModel: PackageIndex) = buildString {
        append(generateHead())
        for (pkg in packageModel) {
            append(generatePackageRow(CrossPPAPackageDescriptor(pkg.key, pkg.value)))
        }
        append(generateTail())
    }

fun generatePackageRow(ppaPackageDesc: CrossPPAPackageDescriptor): String = """
    <div class="row m-2">
      ${generatePackageSummary(ppaPackageDesc)}
        <div class="col-lg-1 col-md-1 col-sm-3 col-xs-2">
          <div class="short-div"></div>
          ${generateReleaseHeader()}
        </div>
        ${generateVersionMatrix(ppaPackageDesc)}
    </div>    
""".trimIndent()

fun generatePackageSummary(pkg: CrossPPAPackageDescriptor): String {
    return """
<div class="card" style="width: 18rem;">
  <div class="card-body">
    <h5 class="card-title">${pkg.packageInfo.name}</h5>
    <h6 class="card-subtitle mb-2 text-muted">${pkg.packageInfo.description}</h6>
  </div>
</div>
    """.trimIndent()
}

//         <div class="col-lg-2 col-md-2 col-sm-3 col-xs-2">
//          <div class="short-div ppa-header">unstable</div>
//          <div class="short-div p-1">1.38</div>
//          <div class="short-div p-1">1.92</div>
//          <div class="short-div p-1">1.99</div>
//        </div>
//        <div class="col-lg-2 col-md-2 col-sm-3 col-xs-2">
//          <div class="short-div ppa-header">stable</div>
//          <div class="short-div p-1">1.38</div>
//          <div class="short-div p-1">1.92</div>
//          <div class="short-div p-1">1.99</div>
//        </div>
//        <div class="col-lg-2 col-md-2 col-sm-3 col-xs-2">
//          <div class="short-div ppa-header">release</div>
//          <div class="short-div p-1">1.38</div>
//          <div class="short-div p-1">1.92</div>
//          <div class="short-div p-1">1.99</div>
//        </div>
fun generateVersionMatrix(ppaPackageDesc: CrossPPAPackageDescriptor) = buildString {
    for ((ppa, ubuntuVersionMap) in ppaPackageDesc.versionMap) {
        append("""
<div class="col-lg-2 col-md-2 col-sm-2 col-xs-2">
  <div class="short-div ppa-header">${ppa.name.toLowerCase()}</div>
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.BIONIC]?.rawVersion)}</div>
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.EOAN]?.rawVersion)}</div>
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.FOCAL]?.rawVersion)}</div>
</div>
""".trimIndent())
    }
}

fun formatVersion(rawVersion: String?) =
    when (rawVersion) {
        null -> ""
        else -> """<span class="badge badge-secondary" style="background-color: ${generateVersionColor(rawVersion)};">$rawVersion</span>"""
    }


fun generateVersionColor(rawVersion: String) =
    "#${rawVersion.hashCode().absoluteValue.toString(16).substring(0..5)}"



//           <div class="short-div release-header p-1">bionic</div>
//          <div class="short-div release-header p-1">eoan</div>
//          <div class="short-div release-header p-1">focal</div>
fun generateReleaseHeader() =
    UbuntuRelease.values().joinToString(separator = "") { descriptor ->
        """<div class="short-div release-header p-1">${descriptor.toString().toLowerCase()}</div>"""
    }

fun generateHead(): String = """
<!doctype html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <title>Regolith Package Versions</title>

  <!-- Bootstrap core CSS -->
  <link href="css/bootstrap.css" rel="stylesheet">
  <!-- Custom styles for this template -->
  <link href="grid.css" rel="stylesheet">
</head>

<body class="py-4">

  <div class="container-fluid">
""".trimIndent()

fun generateTail(): String = """
  </div>

  <footer>
    <div class="footer-copyright text-center py-3">Page generated ${LocalDateTime.now()} - ©2020 Copyright:
        <a href="https://regolith-linux.org">Regolith Linux</a>
    </div>
  </footer>
</body>
</html>    
""".trimIndent()