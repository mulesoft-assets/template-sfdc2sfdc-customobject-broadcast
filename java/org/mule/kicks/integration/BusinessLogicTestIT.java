package org.mule.kicks.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.transport.NullPayload;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * @author damiansima
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkCustomObjectflow;
	private static List<Map<String, String>> createdCustomObjectsInA = new ArrayList<Map<String, String>>();

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
	    // Flow to retrieve custom objects from target system after syncing
		checkCustomObjectflow = getSubFlow("retrieveCustomObjectFlow");
		checkCustomObjectflow.initialise();

		// Create object in target system to be updated 
		SubflowInterceptingChainLifecycleWrapper flowB = getSubFlow("createCustomObjectFlowB");
		flowB.initialise();
		
		List<Map<String, String>> createdCustomObjectsInB = new ArrayList<Map<String,String>>();
        // This custom object should BE synced (updated) as the year is greater than 1968 and the record exists in the target system
		createdCustomObjectsInB.add(aCustomObject()
                        		        .withProperty("Name", "Physical Graffiti")
                        		        .withProperty("interpreter__c", "Lead Zep")
                        		        .withProperty("genre__c", "Hard Rock")
                        		        .build());
		
		flowB.process(getTestEvent(createdCustomObjectsInB, MessageExchangePattern.REQUEST_RESPONSE));

		// Create custom objects in source system to be or not to be synced 
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createCustomObjectFlowA");
		flow.initialise();

		// This custom object should not be synced as the year is not greater than 1968
		createdCustomObjectsInA.add(aCustomObject()
                                        .withProperty("Name", "Are You Experienced")
                                        .withProperty("interpreter__c", "Jimi Hendrix")
                                        .withProperty("year__c", "1967")
                                        .build());

		// This custom object should not be synced as the year is not greater than 1968
		createdCustomObjectsInA.add(aCustomObject()
                                         .withProperty("Name", "Revolver")
                                         .withProperty("interpreter__c", "The Beatles")
                                         .withProperty("year__c", "1966")
                                         .build());

		// This custom object should BE synced (inserted) as the year is greater than 1968 and the record doesn't exist in the target system
		createdCustomObjectsInA.add(aCustomObject()
                                         .withProperty("Name", "Amputechture")
                                         .withProperty("interpreter__c", "The Mars Volta")
                                         .withProperty("year__c", "2006")
                                         .build());
		
		// This custom object should BE synced (updated) as the year is greater than 1968 and the record exists in the target system
		createdCustomObjectsInA.add(aCustomObject()
                        		        .withProperty("Name", "Physical Graffiti")
                        		        .withProperty("interpreter__c", "Led Zeppelin")
                        		        .withProperty("year__c", "1975")
                        		        .build());

		MuleEvent event = flow.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjectsInA.get(i).put("Id", results.get(i).getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		// Delete the created custom objects in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteCustomObjectFromAFlow");
		flow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, String> c : createdCustomObjectsInA) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created custom objects in B
		flow = getSubFlow("deleteCustomObjectFromBFlow");
		flow.initialise();
		
		idList.clear();
		for (Map<String, String> c : createdCustomObjectsInA) {
			Map<String, String> customObject = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, c);
			if (customObject != null) {
				idList.add(customObject.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		Assert.assertEquals("The custom object should not have been sync", null,
				invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(0)));

		Assert.assertEquals("The custom object should not have been sync", null,
				invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(1)));

		Map<String, String> payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(2));
		Assert.assertEquals("The custom object should have been sync", 
		        createdCustomObjectsInA.get(2).get("Name"),
				payload.get("Name"));

		payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(3));
		Assert.assertEquals("The custom object should have been sync (Name)", 
		        createdCustomObjectsInA.get(3).get("Name"),
		        payload.get("Name"));
		Assert.assertEquals("The custom object should have been sync (interpreter__c)", 
		        createdCustomObjectsInA.get(3).get("interpreter__c"),
		        payload.get("interpreter__c"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveCustomObjectFlow(SubflowInterceptingChainLifecycleWrapper flow,
			Map<String, String> customObject) throws Exception {
		Map<String, String> customObjectMap = new HashMap<String, String>();

		customObjectMap.put("Name", customObject.get("Name"));

		MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}
	
	private CustomObjectBuilder aCustomObject() {
	    return new CustomObjectBuilder(); 
	}
	
	private static class CustomObjectBuilder {
	    
	    private Map<String, String> customObject = new HashMap<String, String>();
	    
	    public CustomObjectBuilder withProperty(String key, String value) {
	        customObject.put(key, value);
	        return this;
	    }
	    
	    public Map<String, String> build() {
            return customObject;
        }
	    
	}

}
