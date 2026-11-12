package grails.plugins.export.exporter

import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage

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
        PdfReader reader = new PdfReader(export(new DefaultPDFExporter(), ['pdf.orientation': orientation, title: 'Books']))
        String text = new PdfTextExtractor(reader).getTextFromPage(1)

        then:
        text.contains('Books')
        text.contains('Author')
        text.contains('Beta Title')

        where:
        orientation << ['landscape', 'portrait']
    }

    def "PDF applies every optional parameter"() {
        given: 'a logo image'
        File logo = File.createTempFile('logo', '.png')
        logo.deleteOnExit()
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(image, 'png', logo)

        and: 'the extra header rows report their failures on System.err instead of throwing'
        PrintStream originalErr = System.err
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        System.err = new PrintStream(err, true)

        when:
        PdfReader reader
        try {
            reader = new PdfReader(export(new DefaultPDFExporter(), [
                    title              : 'Books',
                    'pdf.logo'         : logo.absolutePath,
                    'pdf.encoding'     : 'Cp1252',
                    'font.family'      : 'Courier',
                    'title.font.size'  : '14',
                    'title.font.style' : 'bolditalic',
                    'header.font.family': 'Times-Roman',
                    'text.font.size'   : '9',
                    'text.font.style'  : 'italic',
                    'column.widths'    : [0.5, 0.3, 0.2],
                    'header.rows'      : 2,
                    'border.color'     : Color.blue,
                    'separator.color'  : Color.yellow,
                    header             : [['Book', 'Size']],
                    'header.parameters': [[colspan0: 2]]
            ]))
        } finally {
            System.err = originalErr
        }
        String text = new PdfTextExtractor(reader).getTextFromPage(1)

        then:
        err.toString() == ''
        text.contains('Books')
        text.contains('Book')
        text.contains('Size')
        text.contains('Author')
        text.contains('Beta Title')
        reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT)?.size() > 0
    }

    def "PDF leaves out the header row when header.enabled is false"() {
        when:
        PdfReader reader = new PdfReader(export(new DefaultPDFExporter(), ['header.enabled': false]))
        String text = new PdfTextExtractor(reader).getTextFromPage(1)

        then:
        !text.contains('Author')
        text.contains('Beta Title')
    }

    def "PDF writes an empty table for no records"() {
        when:
        DefaultPDFExporter exporter = new DefaultPDFExporter(exportFields: ['title'], parameters: [title: 'Books'])
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        exporter.export(out, [])

        then:
        new PdfTextExtractor(new PdfReader(out.toByteArray())).getTextFromPage(1).contains('Books')
    }

    def "RTF is an RTF document containing the records"() {
        when:
        String rtf = new String(export(new DefaultRTFExporter(), parameters), 'ISO-8859-1')

        then:
        rtf.startsWith('{\\rtf')
        rtf.contains('Books')
        rtf.contains('Beta Title')
        rtf.contains('Author') == headerShown

        where:
        parameters                                                     | headerShown
        [title: 'Books']                                               | true
        [title          : 'Books', 'rtf.encoding': 'Cp1252', 'font.family': 'Courier',
         'title.font.size': '14', 'title.font.style': 'bold', 'text.font.style': 'italic',
         'column.widths': [50, 30, 20], 'header.enabled': false]        | false
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
