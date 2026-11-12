package grails.plugins.export.exporter

import java.awt.Color

import groovy.util.logging.Slf4j

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.HeaderFooter
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter

/**
 * @author Andreas Schmitt
 * 
 */
@Slf4j
class DefaultPDFExporter extends AbstractExporter {
	
	private static Map fontStyles = [bold: Font.BOLD, italic: Font.ITALIC, normal: Font.NORMAL, bolditalic: Font.BOLDITALIC]
	
	protected void exportData(OutputStream outputStream, List data, List<String> fields) throws ExportingException{
		try {
			// Default to landscape in the absence of ['pdf.orientation': 'portrait'].
			Rectangle pageSize = parameters.getOrDefault('pdf.orientation','landscape') == 'portrait' ? PageSize.A4 : PageSize.A4.rotate()
			Document document = new Document(pageSize, 36, 36, 36, 36)
			
			PdfWriter.getInstance(document, outputStream)
			document.open()

            if (getParameters().containsKey(("pdf.logo"))){
                String logoPath = getParameters().get("pdf.logo")
                Image logo = Image.getInstance(logoPath)
                document.add(logo)
            }

			int fontSize = 8
			
			// Default encoding is Latin 1
			String encoding = BaseFont.CP1252 
			// Possible values Cp1250, Cp1252, Cp1257, Identity-H, Identity-V, MacRoman
			if(getParameters().containsKey("pdf.encoding")){
				encoding = getParameters().get("pdf.encoding")
			}
			
			// Title font
			Font title = createFont("title", FontFactory.HELVETICA, encoding, 10, Font.BOLD)
			
			// Header font
			Font header = createFont("header", FontFactory.HELVETICA, encoding, 8, Font.BOLD)
            header.setColor(Color.white)
			
			// Text font
			Font text = createFont("text", FontFactory.HELVETICA, encoding, 8, Font.NORMAL)
			
			Phrase footerPhrase = new Phrase("", text)
			HeaderFooter footer = new HeaderFooter(footerPhrase, true)
			footer.setAlignment(Element.ALIGN_CENTER)
			footer.disableBorderSide(Rectangle.TOP)
			footer.disableBorderSide(Rectangle.BOTTOM)
			document.setFooter(footer)
			
			if(getParameters().containsKey("title")){
				Paragraph paragraph = new Paragraph(getParameters().get("title"), title)
				paragraph.setSpacingAfter(10)
				document.add(paragraph)
			}
			
			PdfPTable table
			if(getParameters().containsKey("column.widths")){
				try {
					List columnWidths = getParameters().get("column.widths")
					float[] tableColumnWidths = new float[columnWidths.size()]
					
					int i = 0
					columnWidths?.each { width ->
						tableColumnWidths[i] = Float.valueOf(width.toString())
						i += 1
					}
					
					table = new PdfPTable(tableColumnWidths)
				}
				catch(Exception e){
					table = new PdfPTable(fields?.size())
				}	
			}
			else {
				table = new PdfPTable(fields?.size())	
			}
			
			table.setWidthPercentage(100)

			if(getParameters().containsKey("header.rows")){
				try {
					table.setHeaderRows(getParameters().get("header.rows"))
				}
				catch(Exception e){
					
				}
			}
			else {
				table.setHeaderRows(1)	
			}
			
			Color borderColor = new Color(163, 163, 163)
			if(getParameters().containsKey("border.color")){
				try {
					borderColor = getParameters().get("border.color")
				}
				catch(Exception e){
					
				}
			}
			
			float minimumCellHeight = 17
			
			// Optional addtional header
			if(getParameters().containsKey("header")){
				try {
					List headerParameters = []
					if(getParameters().containsKey("header.parameters")){
						headerParameters = getParameters().get("header.parameters")
					}
					
					List headers = getParameters().get("header")
					headers?.eachWithIndex { h, k ->
						// Loop over each header
						h?.eachWithIndex { field, i ->
							String value = getLabel(field)
							PdfPCell cell = new PdfPCell(new Paragraph(value, header))
							
							if(headerParameters && headerParameters?.size() > k && headerParameters[k]?.containsKey("colspan" + i)){
								cell.setColspan(headerParameters[k]["colspan" + i])	
							}

							cell.setBorderColor(borderColor)
							cell.setMinimumHeight(minimumCellHeight)
							table.addCell(cell)
						}
					}
				}
				catch(Exception e){
					log.error("Could not add the extra header rows (header: ${getParameters().get('header')})", e)
				}
			}
			
			// Enable/Disable header output
			boolean isHeaderEnabled = true
			if(getParameters().containsKey("header.enabled")){
				isHeaderEnabled = getParameters().get("header.enabled")
			}
			
			// Header columns
			if(isHeaderEnabled){
				fields.each { field ->
					String value = getLabel(field)
					PdfPCell cell = new PdfPCell(new Paragraph(value, header))
                    cell.setBackgroundColor(Color.black)
					cell.setBorderColor(borderColor)
					cell.setMinimumHeight(minimumCellHeight)
					table.addCell(cell)
				}
			}
			
			Color separatorColor = new Color(238, 238, 238)
			if(getParameters().containsKey("separator.color")){
				try {
					separatorColor = getParameters().get("separator.color")
				}
				catch(Exception e){
					
				}
			}
			
			// Data
			if(data){
				data.eachWithIndex { object, index ->				
					fields.each { field ->
						String value = getValue(object, field)?.toString()
						PdfPCell cell = new PdfPCell(new Paragraph(value, text))
						if(index % 2 == 0){
							cell.setBackgroundColor(separatorColor)
						}
						cell.setBorderColor(borderColor)
						cell.setMinimumHeight(minimumCellHeight)
						table.addCell(cell)
					}
				}
			}
			else {
				// Create empty row if empty data is supplied to prevent an exception when DefaultPDFExporter is used standalone
				PdfPCell cell = new PdfPCell(new Paragraph("", text))
				
				cell.setColspan(fields?.size())
				cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER)

				cell.setBackgroundColor(separatorColor)
				cell.setBorderColor(borderColor)
				cell.setMinimumHeight(minimumCellHeight)

				table.addCell(cell)
			}

			document.add(table)
			document.close()
		}
		catch(Exception e){
			throw new ExportingException("Error during export", e)
		}
	}
	
	// Create font from parameters
	private Font createFont(String type, String family, String encoding, int fontSize, int style){
		// Font size
		if(getParameters().containsKey(type + ".font.size")){
			try {
				fontSize = Integer.valueOf(getParameters().get(type + ".font.size").toString())	
			}
			catch(Exception e){
				log.warn("Ignoring ${type}.font.size: ${getParameters().get(type + ".font.size")}", e)
			}
		}
		
		// Global font family
		if(getParameters().containsKey("font.family")){
			family = getParameters().get("font.family")
		}
		
		// Font family
		if(getParameters().containsKey(type + ".font.family")){
			family = getParameters().get(type + ".font.family")
		}
		
		// Font style
		if(getParameters().containsKey(type + ".font.style")){
			try {
				style = fontStyles[getParameters().get(type + ".font.style")]
			}
			catch(Exception e){
				log.warn("Ignoring ${type}.font.style: ${getParameters().get(type + ".font.style")}", e)
			}
		}
		
		// Font encoding (to overwrite global encoding)
		if(getParameters().containsKey(type + ".encoding")){
			encoding = getParameters().get(type + ".encoding")
		}
		
		return FontFactory.getFont(family, encoding, fontSize, style) 
	}
}
