import com.sap.gateway.ip.core.customdev.util.Message
import java.io.InputStream
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row

Message processData(Message message) {
    InputStream inputStream = message.getBody(InputStream.class)
    if (inputStream == null) {
        throw new IllegalArgumentException("Incoming Excel payload body is completely empty.")
    }

    ReadableWorkbook wb = new ReadableWorkbook(inputStream)
    
    // 1. Process Tab 1: Introduction (Index 0)
    def introOptional = wb.getSheet(0)
    if (introOptional != null && introOptional.isPresent()) {
        def introSheet = introOptional.get()
        List<Row> introRows = introSheet.read()
        message.setProperty("Target_FacilityName", introRows.size() > 26 ? introRows.get(26).getCell(5)?.getRawValue() ?: "" : "")
        message.setProperty("Target_Comments", introRows.size() > 28 ? introRows.get(28).getCell(5)?.getRawValue() ?: "" : "")
    }
    
    // 2. Process Tab 2: Sub-basin Selection (Index 1)
    def subBasinOptional = wb.getSheet(1)
    if (subBasinOptional != null && subBasinOptional.isPresent()) {
        def subBasinSheet = subBasinOptional.get()
        List<Row> basinRows = subBasinSheet.read()
        
        // Extract Basin Selection Value from row 25, column B
        String basinValue = ""
        for (Row row : basinRows) {
            if (row.getRowNum() == 25) {
                basinValue = row.getCell(1)?.getRawValue() ?: ""
                break
            }
        }
        message.setProperty("Target_SelectedBasin", basinValue)
        
        // Loop through all data table records starting from physical row 29 onwards
        def targetRowsList = []
        for (Row row : basinRows) {
            int physicalRowNum = row.getRowNum()
            if (physicalRowNum < 29) {
                continue
            }
            
            String countyState = row.getCell(1)?.getRawValue() ?: ""
            String formationType = row.getCell(2)?.getRawValue() ?: ""
            String subBasinId = row.getCell(3)?.getRawValue() ?: ""
            
            String checkCounty = countyState.trim().toUpperCase()
            if (checkCounty.isEmpty() || checkCounty.contains("SELECT THE COUNTY") || checkCounty.contains("RETURN TO") || checkCounty.contains("BACK TO") || checkCounty.contains("CLICK HERE")) {
                continue
            }
            
            if (!formationType.trim().isEmpty() || !subBasinId.trim().isEmpty()) {
                targetRowsList.add([
                    county: countyState.trim(),
                    formation: formationType.trim(),
                    basinId: subBasinId.trim()
                ])
            }
        }
        // Save the clean row dataset directly as a collection object in CPI property context
        message.setProperty("Target_TableRowsObject", targetRowsList)
    }
    
    wb.close()
    return message
}
