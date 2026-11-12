package grails.plugins.export.exporter

import grails.plugins.export.builder.ExcelBuilder
import groovy.util.logging.Slf4j
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors

/**
 * @author Andreas Schmitt
 *
 */
@Slf4j
class DefaultExcelExporter extends AbstractExporter {

    protected void exportData(OutputStream outputStream, List data, List<String> fields) throws ExportingException {
        try {
            def builder = new ExcelBuilder()

            // Enable/Disable header output
            boolean isHeaderEnabled = true
            if (getParameters().containsKey("header.enabled")) {
                isHeaderEnabled = getParameters().get("header.enabled")
            }

            boolean useZebraStyle = false
            if (getParameters().containsKey("zebraStyle.enabled")) {
                useZebraStyle = getParameters().get("zebraStyle.enabled")
            }

            int maxPerSheet = 60000
            if (getParameters().containsKey("max.rows.persheet")) {
                maxPerSheet = getParameters().get("max.rows.persheet")
            }

            def (sheets, limitPerSheet) = computeSheetsAndLimit(data, maxPerSheet)
            def startIndex = 0
            def endIndex = limitPerSheet

            def fileFormat = getParameters().get('fileFormat')

            builder {
                workbook(fileFormat: fileFormat, dateFormat: getParameters().get('dateFormat')){
                    for (int j = 1; j <= sheets; j++) {
                        def dataPerSheet = data.subList(startIndex, endIndex)
                        def sheetTitle = getParameters().get("title")
                        sheet(name: sheetTitle ? "$sheetTitle-$j" : "Export-$j", widths: getParameters().get("column.widths"), numberOfFields: fields.size(), widthAutoSize: getParameters().get("column.width.autoSize")) {

                            format(name: "title") {
                                HorizontalAlignment alignment = HorizontalAlignment.GENERAL
                                if (getParameters().containsKey('titles.alignment')) {
                                    alignment = HorizontalAlignment."${getParameters().get('titles.alignment')}"
                                }
                                font(name: "arial", bold: true, size: 14, alignment: alignment)
                            }

                            format(name: "header") {
                                if (useZebraStyle) {
                                    font(name: "arial", bold: true, backColor: IndexedColors.GREY_80_PERCENT.index, foreColor: IndexedColors.WHITE.index, useBorder: true)
                                } else {
                                    // Use default header format
                                    font(name: "arial", bold: true)
                                }
                            }
                            format(name: "odd") {
                                font(backColor: IndexedColors.GREY_25_PERCENT.index, useBorder: true)
                            }
                            format(name: "even") {
                                font(backColor: IndexedColors.WHITE.index, useBorder: true)
                            }

                            int rowIndex = 0

                            // Option for titles on top of data table
                            def titles = getParameters().get("titles")
                            titles.each {
                                cell(row: rowIndex, column: 0, value: it, format: "title")
                                rowIndex++
                            }

                            //Create header
                            if (isHeaderEnabled) {
                                fields.eachWithIndex { field, index ->
                                    String value = getLabel(field)
                                    cell(row: rowIndex, column: index, value: value, format: "header")
                                }
                                rowIndex++
                            }

                            //Rows
                            dataPerSheet.eachWithIndex { object, k ->
                                String format = useZebraStyle ? ((k % 2) == 0 ? "even" : "odd") : ""
                                fields.eachWithIndex { field, i ->
                                    Object value = getValue(object, field)
                                    cell(row: k + rowIndex, column: i, value: value, format: format)
                                }
                            }

                            if (getParameters().get('titles.mergeCells')) {
                                //Merge title cells
                                titles.eachWithIndex { title, index ->
                                    mergeCells(startColumn: 0, startRow: index, endColumn: fields.size(), endRow: index)
                                }
                            }
                        }

                        startIndex = endIndex
                        endIndex = endIndex + limitPerSheet > data.size() ? data.size() : endIndex + limitPerSheet
                    }
                }
            }

            builder.write(outputStream)
        } catch (Exception e) {
            throw new ExportingException("Error during export", e)
        }
    }

    private static computeSheetsAndLimit(List data, maxPerSheet) {
		if (!data) {
            return [1, 0]
        }

        def limitPerSheet = data.size() > maxPerSheet ? maxPerSheet : data.size()
        def sheetsCount = Math.ceil(data.size()/limitPerSheet)
        log.debug "limitPerSheet:$limitPerSheet ::: sheetsCount:$sheetsCount"
        return [sheetsCount, limitPerSheet]
    }
}
