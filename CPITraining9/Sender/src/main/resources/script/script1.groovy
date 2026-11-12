import com.sap.it.api.pd.PartnerDirectoryService
import com.sap.it.api.ITApiFactory
import com.sap.gateway.ip.core.customdev.util.Message;

def Message processData(Message message) {
    try {
        // Get Partner Directory Service
        def service = ITApiFactory.getApi(PartnerDirectoryService.class, null)
        if (service == null) {
            throw new IllegalStateException("Partner Directory Service not found")
        }

        // Get partner ID (hardcoded for now, or from property)
        def partnerId = message.getProperty("PARTNER_ID")
        // if (partnerId == null || partnerId.isEmpty()) {
        //     partnerId = "PartnerID"  // default partner
        // }
        
        // Fetch String Parameter: Receiver URL
        def receiverUrl = service.getParameter("Address", partnerId, String.class)
        if (receiverUrl == null) {
            throw new IllegalStateException("ReceiverUrl not found for partner: " + partnerId)
        }
        
        // Set as property for dynamic receiver
        message.setProperty("RECEIVER_URL", receiverUrl)
        message.setProperty("PARTNER_ID", partnerId)
        
        def logMessage = "Fetched Partner Directory - Partner: " + partnerId + ", URL: " + receiverUrl
        message.setProperty("LOG_MESSAGE", logMessage)
        
        return message
        
    } catch (Exception e) {
        // Log error
        def errorMessage = "Error fetching Partner Directory: " + e.getMessage()
        message.setProperty("ERROR_MESSAGE", errorMessage)
        throw e
    }
}