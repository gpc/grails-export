package grails.plugins.export.exporter

import groovy.xml.XmlSlurper
import spock.lang.Specification

class DefaultXMLExporterSpec extends Specification {

    static class Plain {
        String title
    }

    static class WithId {
        Long id
        String title
    }

    static class Named {
        Long id
        String title
        String xmlNodeName
    }

    private String export(List records) {
        DefaultXMLExporter exporter = new DefaultXMLExporter(exportFields: ['title'])
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        exporter.export(out, records)
        out.toString('UTF-8')
    }

    def "a record without xmlNodeName or id is exported under its class name"() {
        when:
        def root = new XmlSlurper().parseText(export([new Plain(title: 'Alpha')]))

        then:
        root.name() == 'plains'
        root.plain.title.text() == 'Alpha'
        root.plain.@id.text() == ''
    }

    def "a record's id becomes the id attribute"() {
        when:
        def root = new XmlSlurper().parseText(export([new WithId(id: 7, title: 'Alpha')]))

        then:
        root.withId.@id.text() == '7'
    }

    def "xmlNodeName names the record's element, and the class name is used when it is null"() {
        when:
        def root = new XmlSlurper().parseText(export([
                new Named(id: 1, title: 'Alpha', xmlNodeName: 'book'),
                new Named(id: 2, title: 'Beta')
        ]))

        then:
        root.book.title.text() == 'Alpha'
        root.named.title.text() == 'Beta'
    }

    def "a map record can set xmlNodeName and id as keys"() {
        when:
        def root = new XmlSlurper().parseText(export([[id: 3, title: 'Alpha', xmlNodeName: 'book']]))

        then:
        root.book.@id.text() == '3'
        root.book.title.text() == 'Alpha'
    }

    def "no records give an empty root element, named by xml.root when it is set"() {
        given:
        DefaultXMLExporter exporter = new DefaultXMLExporter(exportFields: ['title'], parameters: parameters)
        ByteArrayOutputStream out = new ByteArrayOutputStream()

        when:
        exporter.export(out, [])
        def root = new XmlSlurper().parseText(out.toString('UTF-8'))

        then:
        root.name() == rootName
        root.children().size() == 0

        where:
        parameters               | rootName
        [:]                      | 'items'
        ['xml.root': 'pilotLogs'] | 'pilotLogs'
    }
}
