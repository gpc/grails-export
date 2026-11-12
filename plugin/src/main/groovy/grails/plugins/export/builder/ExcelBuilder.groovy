package grails.plugins.export.builder


import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andreas Schmitt
 * 
 * This class implements a Groovy builder for creating Excel files. It uses 
 * JExcelApi under the hood to build the actual Excel files. The builder supports 
 * basic formatting and multiple sheets. Formulas and more advanced features are 
 * not supported yet.
 * 
 * The following code snippet shows how to create Excel files using this builder:
 * 
 * def builder = new ExcelBuilder()
 * 
 * builder {
 *     workbook(outputStream: outputStream){
 *     
 *         sheet(name: "Sheet1"){
 *     	       format(name: "format1"){
 *                 font(name: "Arial", size: 10, bold: true, underline: "single", italic: true)    
 *     	       }
 *         
 *             cell(row: 0, column: 0, value: "Hello1")
 *             cell(row: 0, column: 1, value: "Hello2")
 *         }
 *     
 *         sheet(name: "Sheet2"){
 *         
 *         }
 *     }
 * }
 * 
 * builder.write()
 * 
 */

class ExcelBuilder extends BuilderSupport {

	Workbook workbook
	Sheet sheet
	CellStyle defaultHyperlinkStyle
	CellStyle dateCellStyle
	boolean autoSizeColumns

	String format
	Map<String, CellStyle> formats = [:]

	private static Logger log = LoggerFactory.getLogger(ExcelBuilder.class)

	/**
    * This method isn't implemented.
    */
    protected void setParent(Object parent, Object child) {
    }

	/**
	 * This method is invoked when you invoke a method without arguments on the builder 
	 * e.g. builder.write().
	 * 
	 * @param name The name of the method which should be invoked e.g. write.
	 * 
	 */
    protected Object createNode(Object name) {
    	log.debug("createNode(Object name)")
    	log.debug("name: ${name}")
    	createNode(name, [:])
        return null
    }

    /**
     * This method isn't implemented.
     */
    protected Object createNode(Object name, Object value) {
    	log.debug("createNode(Object name, Object value)")
    	log.debug("name: ${name} value: ${value}")
        return null
    }

    /**
     * This method is invoked when you invoke a method with a map of attributes e.g. 
     * cell(row: 0, column: 0, value: "Hello1"). It switches between different build 
     * actions such as creating the workbook, sheet, cells etc.
     * 
     * @param name The name of the method which should be invoked e.g. cell
     * @param attributes The map of attributes which have been supplied e.g. [row: 0, column: 0, value: "Hello1"]
     * 
     */
    protected Object createNode(Object name, Map attributes) {
    	log.debug("createNode(Object name, Map attributes)")
    	log.debug("name: ${name} attributes: ${attributes}")
    	
    	switch (name) {
    		// Workbook, the Excel document as such
    		case "workbook":
				try {
					log.debug("Creating workbook")
					// we support new XLSX file format but default to the old excel 97 file format if not requested otherwise
					workbook = attributes?.fileFormat == 'xlsx' ? new XSSFWorkbook() : new HSSFWorkbook()
					defaultHyperlinkStyle = createDefaultHyperlinkStyle(workbook)
					dateCellStyle = createDateCellStyle(workbook, attributes.dateFormat ?: 'm/d/yy h:mm')
				} catch (Exception e) {
					log.error("Error creating workbook", e)
				}
    			break
    		// Sheet, an Excel file can contain multiple sheets which are typically shown as tabs
    		case "sheet":
    			try {
        			log.debug("Creating sheet")
        			sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(attributes?.name as String))
					if (attributes?.widths && !attributes?.widths?.isEmpty()) {
						attributes.widths.eachWithIndex { width, i ->
							sheet.setColumnWidth(i, (width < 1.0 ? width * 100 : width) as int)
						}
					} else {
                        // Groovy truth, not `as boolean`: on Groovy 5 `null as boolean` throws
                        // IllegalArgumentException instead of giving false, and a sheet without
                        // widthAutoSize (the column.width.autoSize export parameter) passes null.
                        autoSizeColumns = attributes?.widthAutoSize ? true : false
                    }
    			} catch(Exception e) {
    				log.error("Error creating sheet", e)
    			}
    			break
    		// Cell, column header or row cells
    		case "cell":
    			try {
					Row row = sheet.getRow(attributes?.row as int) ?: sheet.createRow(attributes?.row as int)
    				Cell cell
					def value = attributes?.value
					def valueString = attributes?.value?.toString()
        			if (value instanceof Number) {
        				log.debug("Creating number cell")
        				cell = row.createCell(attributes?.column as int, CellType.NUMERIC)
	       			} else if (value instanceof Date) {
                        log.debug("Creating date cell")
                        cell = row.createCell(attributes?.column as int)
						cell.cellStyle = dateCellStyle
                    } else if (valueString?.toLowerCase()?.startsWith('http://') || valueString?.toLowerCase()?.startsWith('https://')) {
						// Create hyperlinks for values beginning with http
						log.debug("Changing cell to Hyperlink")
						Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL)
						link.setAddress(valueString)
						cell = row.createCell(attributes?.column as int)
						cell.setHyperlink(link)
						cell.setCellStyle(defaultHyperlinkStyle)
						value = valueString ?: 'no URL'
					} else {
        				log.debug("Creating label cell")
        				cell = row.createCell(attributes?.column as int, CellType.STRING)
        			}
					cell.setCellValue(value)
					sheet.autoSizeColumn(cell.columnIndex)
					if (attributes?.format && formats.containsKey(attributes?.format)) {
						cell.setCellStyle(formats[attributes.format])
					}
				} catch (Exception e) {
    				log.error("Error adding cell with attributes: ${attributes}", e)
    			}
    			break
    		case "format":
    			if (attributes?.name){
    				format = attributes?.name
    			}
    			break
    		case "font":
    			try {
    				log.debug("attributes: ${attributes}")
    				
        			attributes.name = attributes?.name ? attributes?.name : "arial"
        	    	attributes.italic = attributes?.italic ? attributes?.italic : false
        	    	attributes.bold = attributes?.bold ? Boolean.valueOf(attributes?.bold as String) : false
        	    	attributes["size"] = attributes["size"] ? attributes["size"] : 10
        	    	attributes.underline = attributes?.underline ? attributes?.underline : "none"
                    attributes.foreColor = attributes?.foreColor ? attributes?.foreColor : IndexedColors.BLACK.index
                    attributes.useBorder = attributes?.useBorder ? attributes?.useBorder : false
        	    			
        	    	Map underline = ["none": Font.U_NONE, "double accounting": Font.U_DOUBLE_ACCOUNTING,
        	    			         "single": Font.U_SINGLE, "single accounting": Font.U_SINGLE_ACCOUNTING]
        	    	if (underline.containsKey(attributes.underline)) {
        	    		attributes.underline = underline[attributes.underline]
        	    	}
        	    	Map fontname = ["arial":  "Arial", "courier": "Courier New",
        	    	            "tahoma":  "Tahoma", "times":  "Times New Roman"]
        	    	if (fontname.containsKey(attributes.name)) {
        	    		attributes.name = fontname[attributes.name]
        	    	}
        	    	
        	    	log.debug("attributes: ${attributes}")
        	    			
                    Font font = workbook.createFont()
					font.bold = attributes.bold as boolean
					font.underline = attributes.underline as byte
					font.fontHeightInPoints = attributes.size as short
					font.fontName = attributes.name as String
					font.italic = attributes.italic as boolean
                    font.color = attributes.foreColor as short
                    CellStyle style = workbook.createCellStyle()
					style.font = font

                    if (attributes.useBorder) {
						style.borderBottom = BorderStyle.THIN
						style.borderLeft = BorderStyle.THIN
						style.borderTop = BorderStyle.THIN
						style.borderRight = BorderStyle.THIN
					}
                    if (attributes.backColor) {
						style.setFillBackgroundColor(attributes.backColor as short)
					}
                    if (attributes.alignment) {
						style.alignment = attributes.alignment as HorizontalAlignment
					}
                    formats.put(format, style)
    			} catch (Exception e) {
    				println "Error!"
                    e.printStackTrace();
    			}
    			break
            case "mergeCells":
                log.debug("attributes: ${attributes}")
                try {
                    sheet.mergeCells(attributes?.startColumn, attributes?.startRow, attributes?.endColumn, attributes?.endRow)
                } catch (Exception ex) {
                    log.error("Could not merge cells.  Ensure startColumn, startRow, endColumn, endRow attributes are set.  Attributes: ${attributes}")
                }
                break
    	}
    	
        return null
    }

	private static CellStyle createDefaultHyperlinkStyle(Workbook wb) {
		def style = wb.createCellStyle()
		Font hyperLinkFont = wb.createFont()
		hyperLinkFont.setUnderline(Font.U_SINGLE)
		hyperLinkFont.setColor(IndexedColors.BLUE.getIndex())
		style.setFont(hyperLinkFont)
		return style
	}

	private static CellStyle createDateCellStyle(Workbook wb, String format) {
		def style = wb.createCellStyle()
		style.setDataFormat(wb.creationHelper.createDataFormat().getFormat(format))
		return style
	}

	/**
     * This method isn't implemented.
     */
    protected Object createNode(Object name, Map attributes, Object value) {
    	log.debug("createNode(Object name, Map attributes, Object value)")
    	log.debug("name: ${name} attributes: ${attributes}, value: ${value}")
        return null
    }
    
    /**
     * Finish writing the document.
     */
    void write(OutputStream targetStream) {
    	log.debug("Writing document")
    	try {
        	workbook.write(targetStream)
    	} catch (IOException e) {
    		log.error("Error writing document", e)
    	} finally {
			targetStream.flush()
			targetStream.close()
		}
    }
}
