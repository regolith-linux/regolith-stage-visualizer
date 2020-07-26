
fun generateHtml(packageModel: Map<PackageName, Map<PPADescriptor, Map<UbuntuRelease, PackageVersion>>>) = buildString {
        append(generateHead())
        for (pkg in packageModel) {
            append(generatePackageRow(CrossPPAPackageDescriptor(pkg.key, pkg.value)))
        }

        append(generateTail())
    }

fun generatePackageRow(ppaPackageDesc: CrossPPAPackageDescriptor): String = """
    <div class="row m-2">
      <div class="col-lg-3 col-md-3 col-sm-3 col-xs-3 package-desc">${ppaPackageDesc.packageName.name}</div>
        <div class="col-lg-1 col-md-1 col-sm-3 col-xs-2">
          <div class="short-div"></div>
          ${generateReleaseHeader(ppaPackageDesc)}
        </div>
        ${generateVersionMatrix(ppaPackageDesc)}
    </div>    
""".trimIndent()

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
<div class="col-lg-2 col-md-2 col-sm-3 col-xs-2">
  <div class="short-div ppa-header">${ppa.name}</div>
  <div class="short-div p-1">${ubuntuVersionMap[UbuntuRelease.BIONIC]?.rawVersion}</div>
  <div class="short-div p-1">${ubuntuVersionMap[UbuntuRelease.EOAN]?.rawVersion}</div>
  <div class="short-div p-1">${ubuntuVersionMap[UbuntuRelease.FOCAL]?.rawVersion}</div>
</div>
""".trimIndent())
    }
}



//           <div class="short-div release-header p-1">bionic</div>
//          <div class="short-div release-header p-1">eoan</div>
//          <div class="short-div release-header p-1">focal</div>
fun generateReleaseHeader(ppaPackageDesc: CrossPPAPackageDescriptor) =
    UbuntuRelease.values().joinToString(separator = "") { descriptor ->
        """<div class="short-div release-header p-1">$descriptor</div>"""
    }

fun generateHead(): String = """
<!doctype html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta name="description" content="">
  <meta name="author" content="Mark Otto, Jacob Thornton, and Bootstrap contributors">
  <meta name="generator" content="Jekyll v4.0.1">
  <title>Grid Template · Bootstrap</title>

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

</body>
</html>    
""".trimIndent()