package grails.plugins.conventions

import groovy.transform.CompileStatic

import org.gradle.api.provider.Property

/**
 * Configures the {@code config.docs} convention plugin.
 *
 * Defaults follow the template naming: a root project called {@code grails-x-root} with subprojects
 * {@code grails-x} (the plugin) and {@code grails-x-docs}.
 */
@CompileStatic
abstract class PluginDocsExtension {

    /** Path of the project whose Groovydoc is aggregated. */
    abstract Property<String> getPluginProject()

    /** Path of the project that runs Asciidoctor. */
    abstract Property<String> getDocsProject()
}
