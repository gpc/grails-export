# AGENTS.md - export

## Project Overview

This is a **Grails Plugin** that adds export functionality supporting different formats e.g. CSV, Excel (xls, xlsx),
Open Document Spreadsheet, PDF and XML and can be extended to add additional formats.

- **Language:** Groovy 4.0.30 on Java 17
- **Framework:** Grails 7.x
- **Build System:** Gradle 8.14.4 (with wrapper)
- **Current Version:** see `projectVersion` in `gradle.properties`
- **License:** Apache 2.0

## Skill Files (Best Practices)

Detailed best practices are documented as agent skills in `.agents/skills/` (`.claude` is a symlink to `.agents`).
They are synced from [grails-plugin-template](https://github.com/grails-plugins/grails-plugin-template) — do not edit
them here.

| Skill File                                                                                           | Purpose                                               |
|------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| [`.agents/skills/repository-structure/SKILL.md`](.agents/skills/repository-structure/SKILL.md)       | Canonical directory layout and architectural rules    |
| [`.agents/skills/gradle-best-practices/SKILL.md`](.agents/skills/gradle-best-practices/SKILL.md)     | Gradle best practices, convention plugins, and idioms |
| [`.agents/skills/plugin-project/SKILL.md`](.agents/skills/plugin-project/SKILL.md)                   | Plugin project scope: source code + unit tests only   |
| [`.agents/skills/example-apps/SKILL.md`](.agents/skills/example-apps/SKILL.md)                       | Example app patterns: integration & functional tests  |

The skills describe a local `conventions/` composite build. This repository does not have one — it consumes the
convention plugins as a published artifact instead (see [Convention Plugins](#convention-plugins)).

**Read these skill files before making structural changes to the repository.**

## Critical Rules

1. **NEVER add code to the root `build.gradle` to configure subprojects.** No `subprojects {}`, `allprojects {}`, or
   `configure()` blocks. All shared configuration goes through the `config.*` convention plugins.
2. **The plugin project contains ONLY plugin code and unit tests.** No integration tests, no functional tests, no
   example controllers or views.
3. **Example apps under `examples/` host all integration and functional tests.** They depend on the plugin via
   `implementation project(':grails-export')` and test it as a real consumer would.
4. **Use Gradle convention plugins to deduplicate.** If two or more subprojects share build logic, it belongs in a
   convention plugin in [grails-plugin-template](https://github.com/grails-plugins/grails-plugin-template)'s
   `conventions/` build, not in this repository.
5. **Always use lazy Gradle APIs** to avoid eager initialization (`tasks.register()`, `tasks.named()`, `configureEach`,
   `provider {}`).

## Repository Structure

```
export/
├── .agents/skills/      # Agent skills (synced from grails-plugin-template; `.claude` -> `.agents`)
├── plugin/              # Core Grails plugin (artifact: grails-export)
│   ├── grails-app/      #   Plugin services, taglibs, and conf
│   └── src/main/        #   Plugin source code (exporters, builder, etc.)
├── examples/app1/       # Example Grails app
│   └── grails-app/      #   Controllers and conf for integration testing
├── docs/                # Asciidoctor documentation
├── .github/workflows/   # CI, release, release-notes, update-contributors, update-versions workflows
├── .github/scripts/     # Scripts used by the workflows (verify-repository, update-versions, ...)
├── build.gradle         # Root build file (docs + root-publish ONLY)
├── settings.gradle      # Multi-project settings; maps `config.*` plugins to grails-plugin-conventions
├── gradle.properties    # Version, group and build properties
└── project.yml          # Project metadata: POM info, docs index, contributors, released versions
```

## Build and Test Commands

```bash
# Full build (compile + test)
./gradlew build

# Run only unit tests (plugin module)
./gradlew :grails-export:test

# Run integration tests (example app)
./gradlew :app1:integrationTest

# Skip tests
./gradlew build -PskipTests

# Run the example app
./gradlew :app1:bootRun

# Generate documentation
./gradlew docs

# Clean build
./gradlew clean build

# Publish to the local Maven repository (~/.m2)
./gradlew publishToMavenLocal

# Verify the repository layout (same check as the CI "Verify Repository" job)
groovy .github/scripts/verify-repository.groovy
```

## SDK Requirements

Use SDKMAN to install the correct tool versions (see `.sdkmanrc`):

- Java: `17.0.18-librca`
- Gradle: `8.14.4`
- Groovy: `4.0.30`

Run `sdk env install` to set up the environment.

## Architecture

The plugin provides a service-oriented export mechanism with a tag library for UI integration:

1. **`ExportGrailsPlugin`** registers the `exporterFactory` Spring bean on startup, and loads any custom exporter
   classes declared in `application.groovy` under the `exporters` key.
2. **`ExportService`** is the main entry point for application code. It accepts a format type, output stream (or HTTP
   response), data list, field list, label map, formatter map, and parameter map. It delegates to `exporterFactory`
   to obtain the correct `Exporter` and calls `export()`.
3. **`ExporterFactory`** / **`DefaultExporterFactory`** resolves an `Exporter` implementation by format string (e.g.
   `"csv"`, `"excel"`, `"pdf"`) and configures it with the supplied fields, labels, formatters, and parameters.
4. **`Exporter`** interface / **`AbstractExporter`** — Base contract and shared logic for all format exporters.
5. **`ExportTagLib`** exposes `<export:formats />` (renders an export button bar) and `<export:resource />` (renders
   the CSS `<link>`) for use in GSP views.

### Core Classes

| Class / Interface                             | Location                                                  | Purpose                                                       |
|-----------------------------------------------|-----------------------------------------------------------|---------------------------------------------------------------|
| `ExportGrailsPlugin`                          | `plugin/src/main/groovy/grails/plugins/export/`           | Plugin descriptor; registers beans and custom exporters       |
| `ExportService`                               | `plugin/grails-app/services/grails/plugins/export/`       | Main service; sets response headers, delegates to exporter    |
| `ExportTagLib`                                | `plugin/grails-app/taglib/grails/plugins/export/`         | Tag library: `export:formats`, `export:resource`              |
| `ExporterFactory` / `DefaultExporterFactory`  | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Creates `Exporter` instances by type string                   |
| `Exporter` / `AbstractExporter`               | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exporter interface and shared base class                      |
| `DefaultCSVExporter`                          | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exports data as CSV                                           |
| `DefaultExcelExporter`                        | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exports data as Excel (xls / xlsx) via `ExcelBuilder`         |
| `DefaultODSExporter`                          | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exports data as ODS (Open Document Spreadsheet)               |
| `DefaultPDFExporter`                          | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exports data as PDF                                           |
| `DefaultRTFExporter`                          | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exports data as RTF                                           |
| `DefaultXMLExporter`                          | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Exports data as XML                                           |
| `ExcelBuilder`                                | `plugin/src/main/groovy/grails/plugins/export/builder/`   | Helper for constructing Excel workbooks                       |
| `ExporterUtil`                                | `plugin/src/main/groovy/grails/plugins/export/exporter/`  | Utility methods for exporters                                 |
| `RenderUtils`                                 | `plugin/src/main/groovy/grails/plugins/export/taglib/util/` | Utility methods for tag library resource path resolution    |
| `ExportingException` / `ExporterNotFoundException` | `plugin/src/main/groovy/grails/plugins/export/exporter/` | Export error types                                       |

## Configuration

Custom exporters can be registered in `application.groovy` (or equivalent):

```groovy
exporters {
    myformat = "com.example.export.MyCustomExporter"
}
```

An existing exporter class can be overridden by setting `export.<type>` to a replacement class name.

MIME types for the built-in formats must be declared in `grails.mime.types` (application config):

| Format key  | Typical MIME type                                                      |
|-------------|------------------------------------------------------------------------|
| `csv`       | `text/csv`                                                             |
| `excel`     | `application/vnd.ms-excel`                                             |
| `ods`       | `application/vnd.oasis.opendocument.spreadsheet`                       |
| `pdf`       | `application/pdf`                                                      |
| `rtf`       | `application/rtf`                                                      |
| `xml`       | `text/xml`                                                             |

## Testing

### Unit Tests (`plugin/src/test/`)

Unit tests use the **Spock Framework** and run on JUnit Platform.

### Integration / Functional Tests (`examples/app1/`)

The `TestController` in the example app exercises `ExportService` with real format requests. Integration and
functional tests added here depend on the plugin as a real consumer would.

## Project Metadata

`project.yml` is the single source of project metadata. `config.publish` (via `config.project-metadata`) builds the
Maven POM from it — title, description, organization, license, GitHub slug and developers (from `contributors`) — so
do not add a manual `GrailsPublishExtension` block to `plugin/build.gradle`. It also drives the gh-pages version index
(`docs/src/docs/index.tmpl`).

The `contributors` and `versions` blocks are maintained by the `update-contributors` and `update-versions` workflows.

The Maven group comes from `projectGroup` in `gradle.properties` (`group = projectGroup` in `plugin/build.gradle`).

## Convention Plugins

The `config.*` convention plugins are not part of this repository. `settings.gradle` resolves every plugin id starting
with `config.` to `org.grails.plugins.gradle:grails-plugin-conventions`, published from the `conventions/` build in
[grails-plugin-template](https://github.com/grails-plugins/grails-plugin-template). Changes to shared build logic
go there.

Plugins used in this repository:

| Plugin                  | Applied in               | Purpose                                                                              |
|-------------------------|--------------------------|--------------------------------------------------------------------------------------|
| `config.compile`        | `plugin/`                | Java/Groovy compilation settings (UTF-8, incremental, Java release from `.sdkmanrc`) |
| `config.grails-plugin`  | `plugin/`                | Applies the Grails plugin Gradle plugin, disables Spring dependency management       |
| `config.publish`        | `plugin/`                | Maven publishing metadata from `project.yml` (applies `config.project-metadata`)     |
| `config.testing`        | `plugin/`                | Test framework config (Spock, JUnit Platform, test-logger)                           |
| `config.example-app`    | `examples/app1/`         | Example app config (app-run, compile, grails-assets, testing, grails-web, GSP)       |
| `config.docs`           | root `build.gradle`      | Documentation aggregation (Groovydoc + Asciidoctor)                                  |
| `config.publish-root`   | root `build.gradle`      | Root-level Nexus publishing workaround                                               |

The artifact also provides `config.code-style`, `config.code-coverage`, `config.code-coverage-aggregate` and
`config.grails-web-plugin`, which this repository does not apply yet.

## CI/CD

- **CI** (`.github/workflows/ci.yml`): Builds and tests on push/PR; publishes snapshots to Maven Central Snapshots on
  push to release branches.
- **Release** (`.github/workflows/release.yml`): 4-stage pipeline triggered by GitHub release — stage artifacts, release
  to Maven Central, publish docs to GitHub Pages, bump version.
- **Release Notes** (`.github/workflows/release-notes.yml`): Auto-drafts release notes using release-drafter with
  category labels.
- **Update Contributors / Update Versions** (`.github/workflows/update-contributors.yml`, `update-versions.yml`):
  Write the contributor list and released versions back into `project.yml` and regenerate the gh-pages index.

All workflows and `.github/scripts/` are synced from grails-plugin-template. To keep a local change, add a
`<filename>.lock` file next to it.

## Code Conventions

- Groovy source files use standard Grails conventions (services and taglibs in `grails-app/`, other classes in
  `src/main/groovy/`).
- **Use `def` for local variables** where the type is inferred from the right-hand side (e.g., constructor calls,
  method calls, casts, factory methods). Explicit types should only be used for local variables when the type cannot
  be inferred or when needed for `@CompileStatic` compilation. This applies to both production code and tests.
- When writing Gradle, always use the latest best practices to avoid eager initialization.
