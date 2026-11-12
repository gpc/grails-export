package grails.plugins.export.builder

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import spock.lang.Specification

class ExcelBuilderSpec extends Specification {

    ListAppender<ILoggingEvent> appender = new ListAppender<>()
    Logger builderLog = LoggerFactory.getLogger(ExcelBuilder) as Logger

    def setup() {
        appender.start()
        builderLog.addAppender(appender)
    }

    def cleanup() {
        builderLog.detachAppender(appender)
    }

    def "a sheet without widths or widthAutoSize is created without an error"() {
        given:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ExcelBuilder builder = new ExcelBuilder()

        when:
        builder {
            workbook {
                sheet(name: 'Sheet1') {
                    cell(row: 0, column: 0, value: 'Hello')
                }
            }
        }
        builder.write(out)

        then:
        appender.list.findAll { it.level == Level.ERROR }*.formattedMessage == []
        !builder.autoSizeColumns
        WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray())).getSheet('Sheet1')
                .getRow(0).getCell(0).stringCellValue == 'Hello'
    }

    def "widthAutoSize turns column auto-sizing on or off"() {
        given:
        ExcelBuilder builder = new ExcelBuilder()

        when:
        builder {
            workbook {
                sheet(name: 'Sheet1', widthAutoSize: value)
            }
        }

        then:
        builder.autoSizeColumns == expected
        appender.list.findAll { it.level == Level.ERROR } == []

        where:
        value  | expected
        true   | true
        false  | false
        null   | false
        'true' | true
    }
}
