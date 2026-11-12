[![Maven Central](https://img.shields.io/maven-central/v/io.github.gpc/grails-export)](https://central.sonatype.com/artifact/io.github.gpc/grails-export)
[![License](https://img.shields.io/github/license/gpc/grails-export)](https://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://github.com/gpc/grails-export/actions/workflows/ci.yml/badge.svg?event=push)](https://github.com/gpc/grails-export/actions/workflows/ci.yml)

Grails Export Plugin
====================

The official Grails Export Plugin.

This plugin offers export functionality supporting different formats e.g. CSV, Excel (xls, xlsx),
Open Document Spreadsheet, PDF and XML and can be extended to add additional formats.

The user guide can be found here: 📚 [Documentation]

This was previously forked from [Nathan Wells]

## Installation

Add the following dependency to the `build.gradle` file:

### Grails 8.x

```
dependencies {
    implementation("io.github.gpc:grails-export:8.0.0-RC1")
}
```

Grails 8 has three lines of this plugin. They differ only in the PDF, RTF and Excel libraries they bring with them
(and which your application gets too); the plugin's own tags, exporters and parameters are the same in all three.

| Line | Branch | Status | Apache POI | OpenPDF / OpenRTF | OpenPDF package |
|------|--------|--------|------------|-------------------|-----------------|
| 8.0  | [`8.0.x`](https://github.com/gpc/grails-export/tree/8.0.x) | current | 5.5 | 1.4 / 1.2 | `com.lowagie` |
| 8.1  | [`8.1.x`](https://github.com/gpc/grails-export/tree/8.1.x) | in development, to be released with Grails 8.1 | 6.0 | 2.4 / 2.0 | `com.lowagie` |
| 9.0  | [`9.0.x`](https://github.com/gpc/grails-export/tree/9.0.x) | in development | 6.0 | 3.0 / 3.0 | `org.openpdf`, `org.openrtf` |

What changes for an application, if it uses these libraries itself:

* **8.0 → 8.1**: Apache POI 6 and OpenPDF 2. Every POI module the application declares (`poi-scratchpad`, for
  example) must move to 6.0 as well. OpenPDF 2.0 removed 108 methods and fields deprecated in 1.3.40, so code using one of
  them must use its replacement ([OpenPDF 2.0.0 release notes](https://github.com/LibrePDF/OpenPDF/releases/tag/2.0.0)).
* **8.1 → 9.0**: OpenPDF 3 and OpenRTF 3 move their classes out of `com.lowagie`, so application code importing
  `com.lowagie.text.*` changes its imports to `org.openpdf.text.*` (and `com.lowagie.text.rtf.*` to
  `org.openrtf.text.rtf.*`).

An application that only uses the plugin's tags and `exportService` needs no changes between them.

Snapshots of each line (`8.0.0-SNAPSHOT`, `8.1.0-SNAPSHOT`, `9.0.0-SNAPSHOT`) are published from its branch to the
Sonatype snapshot repository:

```
repositories {
    maven {
        url = 'https://central.sonatype.com/repository/maven-snapshots/'
        mavenContent { snapshotsOnly() }
        content { includeGroup('io.github.gpc') }
    }
}
```

### Grails 7.x

```
dependencies {
    implementation("io.github.gpc:grails-export:7.1.0")
}
```

### Grails 5.x - 6.x

```
dependencies {
    implementation("org.grails.plugins:export:2.0.0")
}
```

### Grails 3.x - 4.x

```
dependencies {
    compile("org.grails.plugins:export:2.0.0")
}
```

[Documentation]: https://gpc.github.io/grails-export/
[Nathan Wells]: https://github.com/nwwells/grails-export
