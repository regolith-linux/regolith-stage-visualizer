import java.time.LocalDateTime
import java.util.*
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
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.bionic])}</div>
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.eoan])}</div>
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.focal])}</div>
  <div class="short-div p-1">${formatVersion(ubuntuVersionMap[UbuntuRelease.groovy])}</div>
</div>
""".trimIndent())
    }
}

fun formatVersion(pkgInfo: PPAPackageInfo?, id: Long = Random().nextLong()) =
    when (pkgInfo) {
        null -> ""
        else -> """
            <button class="btn btn-sm" type="button" data-toggle="collapse" data-target="#changelog-$id" aria-expanded="false" aria-controls="collapseExample" style="background-color: ${generateVersionColor(pkgInfo.version)};">
            ${pkgInfo.version}
            </button>
            <div class="collapse" id="changelog-$id">
              <div class="card card-body" style="z-index: 1;">
                ${org.apache.commons.text.StringEscapeUtils.escapeHtml4(filterPkgInfo(pkgInfo.changeLog))}
              </div>
            </div>
        """.trimIndent()
    }

fun filterPkgInfo(changeLog: String?): String? {
    if (changeLog == null) return null

    return changeLog.split("\n\n").filterIndexed { index, line -> index != 0 && !line.startsWith(" -- ") }.joinToString(separator = "<br>") { it }
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
  <!-- Optional JavaScript -->
  <!-- jQuery first, then Popper.js, then Bootstrap JS -->
  <script src="https://code.jquery.com/jquery-3.3.1.slim.min.js" integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" crossorigin="anonymous"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js" integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1" crossorigin="anonymous"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" crossorigin="anonymous"></script>
</body>
</html>    
""".trimIndent()