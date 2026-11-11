import com.sap.gateway.ip.core.customdev.util.Message
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

Message processData(Message message) {
    // 1. Retrieve the parsed variables securely from the CPI Property context
    String facilityName  = message.getProperty("Target_FacilityName") ?: ""
    String userComments  = message.getProperty("Target_Comments") ?: ""
    String selectedBasin = message.getProperty("Target_SelectedBasin") ?: ""
    def tableRows        = message.getProperty("Target_TableRowsObject") as List ?: []
    
    ClassLoader classLoader = this.getClass().getClassLoader()
    InputStream jarResourceStream = classLoader.getResourceAsStream("target_template.xlsx")
    
    if (jarResourceStream == null) {
        throw new FileNotFoundException("Template 'target_template.xlsx' missing from resource jar directory.")
    }
    
    ZipInputStream zipIn = new ZipInputStream(jarResourceStream)
    ByteArrayOutputStream bos = new ByteArrayOutputStream()
    ZipOutputStream zipOut = new ZipOutputStream(bos)
    ZipEntry entry = zipIn.getNextEntry()
    
    while (entry != null) {
        zipOut.putNextEntry(new ZipEntry(entry.getName()))
        
        if (entry.getName().equals("xl/sharedStrings.xml")) {
            // --- STEP A: Swap Static Headers & Dropdown ---
            ByteArrayOutputStream stringBytes = new ByteArrayOutputStream()
            copyStream(zipIn, stringBytes)
            String content = stringBytes.toString("UTF-8")
            
            content = content.replace("##FACILITY_NAME##", escapeXml(facilityName))
            content = content.replace("##USER_COMMENTS##", escapeXml(userComments))
            content = content.replace("##SELECTED_BASIN##", escapeXml(selectedBasin))
            
            zipOut.write(content.getBytes("UTF-8"))
            
        } else if (entry.getName().equals("xl/worksheets/sheet2.xml")) {
            // --- STEP B: Multi-Row Stream-Safe Table Generator ---
            ByteArrayOutputStream sheetBytes = new ByteArrayOutputStream()
            copyStream(zipIn, sheetBytes)
            String sheetContent = sheetBytes.toString("UTF-8")
            
            // Clean up any stray row parameters from row 38 onwards to prevent sorting collisions
            sheetContent = sheetContent.replaceAll("<row r=\"(3[89]|[4-9]\\d|\\d{3,})\".*?</row>", "")
            sheetContent = sheetContent.replace("##DELETE_MARKER##", "")
            
            // Generate clean inline OpenXML rows for your dynamic table list items
            StringBuilder rowsMarkup = new StringBuilder()
            tableRows.eachWithIndex { rowData, index ->
                int currentExcelRow = 38 + index // 38, 39, 40... perfectly sorted chronological layout
                
                rowsMarkup.append("<row r=\"${currentExcelRow}\" spans=\"1:4\">")
                rowsMarkup.append("<c r=\"B${currentExcelRow}\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(rowData.county)}</t></is></c>")
                rowsMarkup.append("<c r=\"C${currentExcelRow}\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(rowData.formation)}</t></is></c>")
                rowsMarkup.append("<c r=\"D${currentExcelRow}\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(rowData.basinId)}</t></is></c>")
                rowsMarkup.append("</row>")
            }
            
            // Safely inject your dynamic row nodes right before the data sheet ends
            int sheetDataEndIdx = sheetContent.indexOf("</sheetData>")
            if (sheetDataEndIdx != -1) {
                sheetContent = sheetContent.substring(0, sheetDataEndIdx) + rowsMarkup.toString() + sheetContent.substring(sheetDataEndIdx)
            } else if (sheetContent.contains("<sheetData/>")) {
                sheetContent = sheetContent.replace("<sheetData/>", "<sheetData>" + rowsMarkup.toString() + "</sheetData>")
            }
            
            zipOut.write(sheetContent.getBytes("UTF-8"))
        } else {
            copyStream(zipIn, zipOut)
        }
        
        zipIn.closeEntry()
        zipOut.closeEntry()
        entry = zipIn.getNextEntry()
    }
    
    zipOut.close()
    zipIn.close()
    jarResourceStream.close()
    
    // 3. Return the fully compliant binary workbook stream back to the Camel message body
    message.setBody(bos.toByteArray())
    message.setHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    
    return message
}

void copyStream(InputStream input, OutputStream output) {
    byte[] buffer = new byte[4096]
    int bytesRead
    while ((bytesRead = input.read(buffer)) != -1) {
        output.write(buffer, 0, bytesRead)
    }
}

String escapeXml(String input) {
    if (input == null) return ""
    return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
