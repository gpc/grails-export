package grails.plugins.export.exporter

import com.lowagie.text.Cell
import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Table
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.rtf.RtfWriter2
import com.lowagie.text.HeaderFooter
import com.lowagie.text.Phrase
import com.lowagie.text.Element
import com.lowagie.text.Rectangle
import groovy.util.logging.Slf4j
import java.awt.Color


/**
 * @author Andreas Schmitt
 * 
 */
@Slf4j
public class DefaultRTFExporter extends AbstractExporter {
	
	private static Map fontStyles = [bold: Font.BOLD, italic: Font.ITALIC, normal: Font.NORMAL, bolditalic: Font.BOLDITALIC]
	
	protected void exportData(OutputStream outputStream, List data, List<String> fields) throws ExportingException{
		try {
			Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36)
			
			RtfWriter2.getInstance(document, outputStream)
			document.open()
			
			int fontSize = 8
			
			// Default encoding is Latin 1
			String encoding = BaseFont.CP1252 
			// Possible values Cp1250, Cp1252, Cp1257, Identity-H, Identity-V, MacRoman
			if(getParameters().containsKey("rtf.encoding")){
				encoding = getParameters().get("rtf.encoding")
			}
			
			// Title font
			Font title = createFont("title", FontFactory.HELVETICA_BOLD, encoding, 10, Font.BOLD)
			
			// Header font
			Font header = createFont("header", FontFactory.HELVETICA_BOLD, encoding, 8, Font.BOLD)
			
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
			
			Table table = new Table(fields?.size())
			if(getParameters().containsKey("column.widths")){
				try {
					List columnWidths = getParameters().get("column.widths")
					float[] tableColumnWidths = new float[columnWidths.size()]
					
					int i = 0
					columnWidths?.each { width ->
						tableColumnWidths[i] = Float.valueOf(width.toString())
						i += 1
					}
					
					table.setWidths(tableColumnWidths)
				}
				catch(Exception e){
					
				}	
			}
			
			Color borderColor = new Color(163, 163, 163)
			float minimumCellHeight = 17
			
			// Enable/Disable header output
			boolean isHeaderEnabled = true
			if(getParameters().containsKey("header.enabled")){
				isHeaderEnabled = getParameters().get("header.enabled")
			}
			
			// Create header
			if(isHeaderEnabled){
				fields.each { field ->
					String value = getLabel(field)
					Cell cell = new Cell(new Paragraph(value, header))
					cell.setBorderColor(borderColor)
					table.addCell(cell)
				}
			}
			
			Color separatorColor = new Color(238, 238, 238)
			
			data.eachWithIndex { object, index ->				
				fields.each { field ->
					String value = getValue(object, field)?.toString()
					Cell cell = new Cell(new Paragraph(value, text))
					if(index % 2 == 0){
						cell.setBackgroundColor(separatorColor)
					}
					cell.setBorderColor(borderColor)
					table.addCell(cell)
				}
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
			family = getParameters().get(type + ".encoding")
		}
		
		return FontFactory.getFont(family, encoding, fontSize, style) 
	}
}
