package grails.plugins.export

import org.slf4j.LoggerFactory

import grails.plugins.Plugin

class ExportGrailsPlugin extends Plugin {
    def grailsVersion = "7.0.0 > *"
    def dependsOn = [:]

    def title = "Grails Export Plugin"
    def documentation = "https://gpc.github.io/grails-export/"
    def author = "Grails Plugin Collective"
    def authorEmail = "grails.plugin.collective@gmail.com"
    def description = '''\
This plugin offers export functionality supporting different formats e.g. CSV, Excel, Open Document Spreadsheet, PDF and XML 
and can be extended to add additional formats. 
'''
    def license = 'APACHE'
    def organization = [name: 'Grails Plugin Collective', url: 'https://github.com/gpc']
    def issueManagement = [system: 'Github', url: 'https://github.com/gpc/grails-export/issues']
    def scm = [url: 'https://github.com/gpc/grails-export']


    Closure doWithSpring() {
        { ->
            //This is only necessary here, because later on log is injected by Spring
            def log = LoggerFactory.getLogger(ExportGrailsPlugin.class)

            "exporterFactory"(grails.plugins.export.exporter.DefaultExporterFactory)

            try {
                grailsApplication.config.exporters.each { key, value ->
                    try {
                        //Override default renderer configuration
                        if (grailsApplication.config?.export."${key}") {
                            value = grailsApplication.config.export."${key}"
                        }

                        Class clazz = Class.forName(value, true, new GroovyClassLoader())

                        //Add to spring
                        "$key"(clazz) { bean ->
                            bean.scope = "prototype"
                        }
                    } catch (ClassNotFoundException e) {
                        log.error("Couldn't find class: ${value}", e)
                    }
                }
            } catch (Exception e) {
                log.error("Error initializing Export plugin", e)
            }
            catch (Error e) {
                //Strange error which happens when using generate-all and hibernate.cfg
                log.error("Error initializing Export plugin")
            }
        }
    }
}
