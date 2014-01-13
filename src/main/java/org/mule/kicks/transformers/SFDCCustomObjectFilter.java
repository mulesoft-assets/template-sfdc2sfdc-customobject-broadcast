package org.mule.kicks.transformers;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.NullPayload;

/**
 * The purpose of this class is to decide whether or not a contact should be
 * sync. Provided two contacts one from Org A (source) and one from Org B
 * (destination) the class will decided which one to use.
 * 
 * The newest Last modified date will win. Should no contact in Org B is
 * provided then the contact in Org A will be sync.
 * 
 * @author damiansima
 */
public class SFDCCustomObjectFilter extends AbstractMessageTransformer {
    private static final String ID_FIELD = "Id";
	private static final String FIELD_TYPE = "type";
	private static final String CUSTOM_OBJECT_IN_COMPANY_B = "customObjectInB";
	private static final String LAST_MODIFIED_DATE = "LastModifiedDate";
	private static final String FILTERED_CUSTOM_OBJECTS_COUNT = "filteredCustomObjectsCount";

	private static final String CUSTOM_FIELD_1 = "year__c";

    @Override
    @SuppressWarnings("unchecked")
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		Map<String, String> customObjectInA = (Map<String, String>) message.getPayload();

		// If custom object in B doesn't exist, the operation will be a new insert
		if (message.getInvocationProperty(CUSTOM_OBJECT_IN_COMPANY_B) instanceof NullPayload) {
			customObjectInA.remove(FIELD_TYPE);
			customObjectInA.remove(LAST_MODIFIED_DATE);
			
			// Remove custom fields from source system which don't exist in the target system
			customObjectInA.remove(CUSTOM_FIELD_1);
		} else {
			Map<String, String> customObjectInB = message.getInvocationProperty(CUSTOM_OBJECT_IN_COMPANY_B);

			DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			DateTime lastModifiedDateOfA = formatter.parseDateTime(customObjectInA.get(LAST_MODIFIED_DATE));
			DateTime lastModifiedDateOfB = formatter.parseDateTime(customObjectInB.get(LAST_MODIFIED_DATE));

			// If the Last Updated Date in the source system is after the one in the target system, we'll update the record
			if (lastModifiedDateOfA.isAfter(lastModifiedDateOfB)) {
				customObjectInA.remove(FIELD_TYPE);
				customObjectInA.remove(LAST_MODIFIED_DATE);
				// Add Id field to the record corresponding to the Id in the target system
				customObjectInA.put(ID_FIELD, customObjectInB.get(ID_FIELD));

				// Remove custom fields from source system which don't exist in the target system
				customObjectInA.remove(CUSTOM_FIELD_1);
			} else {
			    // We don't update the record in the target system as it has a Last Updated Date posterior to the one in the source system
				customObjectInA = null;
				increaseFilteredContactsCount(message);
			}
		}
		message.setPayload(customObjectInA);
		return message;
	}

	private void increaseFilteredContactsCount(MuleMessage message) {
		Integer filteredCount = (Integer) message.getInvocationProperty(FILTERED_CUSTOM_OBJECTS_COUNT);
		if (filteredCount == null) {
			filteredCount = 0;
		}
		filteredCount++;
		message.setInvocationProperty(FILTERED_CUSTOM_OBJECTS_COUNT, filteredCount);
	}

}
