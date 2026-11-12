package script

/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en-US/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en-US/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.xml.MarkupBuilder

def Message processData(Message message) {

    def headersMap = message.getHeaders()
    String propertyData = headersMap.get("ListOfArtifacts") ?: ""
    
    List<String> itemsList = propertyData.split(",").collect { it.trim() }
    

    def writer = new StringWriter()
    def xmlBuilder = new MarkupBuilder(writer)
    
    // Build the requested XML structure
    xmlBuilder.Artifacts {
        itemsList.each { itemValue ->
            if (itemValue) { 
                Artifact(itemValue)
            }
        }
    }
    
    // 5. Overwrite the CPI body with the new XML
    message.setBody(writer.toString())

    return message;
}