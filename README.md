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
    implementation("io.github.gpc:grails-export:8.0.0")
}
```

### Grails 7.x

```
dependencies {
    implementation("io.github.gpc:grails-export:7.0.0")
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
