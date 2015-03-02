package de.andreasschmitt.export.exporter

import org.w3c.dom.Element
import org.odftoolkit.odfdom.pkg.OdfFileDom
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle
import org.odftoolkit.odfdom.doc.table.*
import org.odftoolkit.odfdom.dom.element.table.*
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph
import org.odftoolkit.odfdom.pkg.OdfNamespace
import org.odftoolkit.odfdom.type.ValueType
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.odftoolkit.odfdom.pkg.OdfPackage
import org.w3c.dom.Document

/**
 * @author Andreas Schmitt
 * 
 */
class DefaultODSExporter  extends AbstractExporter {
	
	protected void exportData(OutputStream outputStream, List data, List fields) throws ExportingException {
		try {
			 OdfSpreadsheetDocument spreadsheetDocument = OdfSpreadsheetDocument.createSpreadsheetDocument()
			
			 OdfFileDom contentDom = spreadsheetDocument.getContentDom()

			 NodeList nodeList = contentDom.getElementsByTagNameNS(OdfTable.ELEMENT_NAME.getUri(), OdfTable.ELEMENT_NAME.getLocalName())
			 OdfTable table = (OdfTable) nodeList.item(0)
			 
			 table.removeChild(table.getFirstChild().getNextSibling())
			 
			 // Enable/Disable header output
			 boolean isHeaderEnabled = true
			 if(getParameters().containsKey("header.enabled")){
				 isHeaderEnabled = getParameters().get("header.enabled")
			 }
			 
			 // Create header
			 if(isHeaderEnabled){
				 TableTableHeaderRowsElement tableHeaderRows = new TableTableHeaderRowsElement(contentDom)
				 OdfTableRow headerRow = new OdfTableRow(contentDom)
			 			 
				 //Header
				 fields.each { field ->		     	
			     	String label = getLabel(field) 
			     	
			        OdfTableCell cell = new OdfTableCell(contentDom)
			        cell.setStringValue(label)
			        cell.setValueType(OdfValueType.STRING)

                    OdfTextParagraph para = new OdfTextParagraph(contentDom)
			        para.appendChild(contentDom.createTextNode(label))
			        
			        cell.appendChild(para)
			        headerRow.appendChild(cell)
			        tableHeaderRows.appendChild(headerRow)
				 }
			     table.appendChild(tableHeaderRows)
			 }
			 
		     //Rows
		     data.each { object ->
		     	OdfTableRow tr = new OdfTableRow(contentDom)
					
				fields.each { field ->
					Object value = getValue(object, field)
					
					OdfTableCell cell = new OdfTableCell(contentDom)					
			        cell.setStringValue(value?.toString())
			        cell.setValueType(OdfValueType.STRING)

                    OdfTextParagraph para = new OdfTextParagraph(contentDom)
			        para.appendChild(contentDom.createTextNode(value?.toString()))
			        
			        cell.appendChild(para)
			        tr.appendChild(cell)
				}
					
				table.appendChild(tr)
		     }
			 
			 spreadsheetDocument.save(outputStream)
		}
		catch(Exception e){
			throw new ExportingException("Error during export", e)
		}
	}
}
