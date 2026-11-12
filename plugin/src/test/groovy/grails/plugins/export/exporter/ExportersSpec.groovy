package grails.plugins.export.exporter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.slf4j.LoggerFactory
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

    ListAppender<ILoggingEvent> appender = new ListAppender<>()
    List<Logger> exporterLogs = [DefaultPDFExporter, DefaultRTFExporter].collect { LoggerFactory.getLogger(it) as Logger }

    def setup() {
        appender.start()
        exporterLogs*.addAppender(appender)
    }

    def cleanup() {
        exporterLogs*.detachAppender(appender)
    }

    /** What the PDF and RTF exporters logged at WARN or above: they log a setting they could not apply and carry on. */
    private List<String> warnings() {
        appender.list.findAll { it.level.isGreaterOrEqual(Level.WARN) }*.formattedMessage
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

        and: 'a setting that fails is logged (or, before the exporters had a logger, printed) and skipped'
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
        warnings() == []
        text.contains('Books')
        text.contains('Book')
        text.contains('Size')
        text.contains('Author')
        text.contains('Beta Title')
        reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT)?.size() > 0
    }

    def "a font setting that cannot be applied is logged and skipped"() {
        when:
        byte[] document = export(exporter, ['text.font.size': 'large', 'text.font.style': 'shiny'])

        then:
        document.length > 0
        warnings() == ['Ignoring text.font.size: large', 'Ignoring text.font.style: shiny']

        where:
        exporter << [new DefaultPDFExporter(), new DefaultRTFExporter()]
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
        warnings() == []

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
