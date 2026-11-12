package grails.plugins.export.exporter

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import spock.lang.Specification

/**
 * Runs each exporter on the same rows and reads the result back with the library that wrote it,
 * so an update to POI, OpenPDF, OpenRTF or odfdom that breaks an exporter fails here.
 */
class ExportersSpec extends Specification {

    static class Book {
        Long id
        String title
        String author
        Integer pages
    }

    List<Book> books = [
            new Book(id: 1, title: 'Alpha Title', author: 'Ann', pages: 10),
            new Book(id: 2, title: 'Beta Title', author: 'Bob', pages: 20)
    ]

    private byte[] export(Exporter exporter, Map parameters = [:]) {
        exporter.exportFields = ['title', 'author', 'pages']
        exporter.labels = [title: 'Title', author: 'Author', pages: 'Pages']
        exporter.parameters = parameters
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        exporter.export(out, books)
        out.toByteArray()
    }

    def "Excel writes a header row and one row per record"() {
        when:
        def sheet = WorkbookFactory.create(new ByteArrayInputStream(export(new DefaultExcelExporter(), parameters)))
                .getSheetAt(0)

        then:
        sheet.getRow(0).getCell(0).stringCellValue == 'Title'
        sheet.getRow(2).getCell(1).stringCellValue == 'Bob'

        where:
        parameters << [
                ['column.width.autoSize': true],
                ['column.width.autoSize': false, 'column.widths': [0.3, 0.3, 0.2]]
        ]
    }

    def "PDF contains the records' text"() {
        when:
        PdfReader reader = new PdfReader(export(new DefaultPDFExporter(), ['pdf.orientation': 'landscape', title: 'Books']))

        then:
        new PdfTextExtractor(reader).getTextFromPage(1).contains('Beta Title')
    }

    def "RTF is an RTF document containing the records"() {
        when:
        String rtf = new String(export(new DefaultRTFExporter(), [title: 'Books']), 'ISO-8859-1')

        then:
        rtf.startsWith('{\\rtf')
        rtf.contains('Beta Title')
    }

    def "ODS writes one row per record after the header"() {
        when:
        def doc = OdfSpreadsheetDocument.loadDocument(new ByteArrayInputStream(export(new DefaultODSExporter())))

        then:
        doc.tableList[0].getCellByPosition(1, 2).stringValue == 'Bob'
    }

    def "CSV and XML contain the records"() {
        expect:
        new String(export(new DefaultCSVExporter()), 'UTF-8').contains('Beta Title')
        new String(export(new DefaultXMLExporter()), 'UTF-8').contains('Beta Title')
    }
}
