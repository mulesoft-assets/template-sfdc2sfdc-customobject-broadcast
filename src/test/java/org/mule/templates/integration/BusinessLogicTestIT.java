package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;
import static org.mule.templates.builders.SfdcObjectBuilder.aCustomObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Template that make calls to external systems.
 * 
 */
public class BusinessLogicTestIT extends AbstractTemplateTestCase {

	private static SubflowInterceptingChainLifecycleWrapper retrieveCustomObjectFromBFlow;
	private static List<Map<String, Object>> createdCustomObjectsInA = new ArrayList<Map<String, Object>>();

	private BatchTestHelper helper;

	@Before
	public void setUp() throws Exception {
		helper = new BatchTestHelper(muleContext);
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();

		// Flow to retrieve custom objects from target system after syncing
		retrieveCustomObjectFromBFlow = getSubFlow("retrieveCustomObjectFlow");
		retrieveCustomObjectFromBFlow.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		// Assert first object was not sync
		assertEquals("The custom object should not have been sync", null, invokeRetrieveFlow(retrieveCustomObjectFromBFlow, createdCustomObjectsInA.get(0)));

		// Assert second object was not sync
		assertEquals("The custom object should not have been sync", null, invokeRetrieveFlow(retrieveCustomObjectFromBFlow, createdCustomObjectsInA.get(1)));

		// Assert third object was created in target system
		Map<String, Object> payload = invokeRetrieveFlow(retrieveCustomObjectFromBFlow, createdCustomObjectsInA.get(2));
		assertEquals("The custom object should have been sync", createdCustomObjectsInA.get(2)
																						.get("Name"), payload.get("Name"));

		// Assert fourth object was updated in target system
		Map<String, Object> fourthCustomObject = createdCustomObjectsInA.get(3);
		payload = invokeRetrieveFlow(retrieveCustomObjectFromBFlow, fourthCustomObject);
		assertEquals("The custom object should have been sync (Name)", fourthCustomObject.get("Name"), payload.get("Name"));
		assertEquals("The custom object should have been sync (interpreter__c)", fourthCustomObject.get("interpreter__c"), payload.get("interpreter__c"));
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
		// Create object in target system to be updated
		List<Map<String, Object>> createdCustomObjectsInB = new ArrayList<Map<String, Object>>();

		SubflowInterceptingChainLifecycleWrapper createCustomObjectFlowB = getSubFlow("createCustomObjectFlowB");
		createCustomObjectFlowB.initialise();

		// This custom object should BE sync (updated) as the year is greater
		// than 1968 and the record exists in the target system
		createdCustomObjectsInB.add(aCustomObject().with("Name", buildUniqueName("Physical Graffiti"))
													.with("interpreter__c", "Lead Zep")
													.build());

		createCustomObjectFlowB.process(getTestEvent(createdCustomObjectsInB, MessageExchangePattern.REQUEST_RESPONSE));

		// Create custom objects in source system to be or not to be synced
		final SubflowInterceptingChainLifecycleWrapper createCustomObjectFlowA = getSubFlow("createCustomObjectFlowA");
		createCustomObjectFlowA.initialise();

		// This custom object should not be synced as the year is not greater
		// than 1968
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Are You Experienced"))
													.with("interpreter__c", "Jimi Hendrix")
													.with("year__c", "1967")
													.build());

		// This custom object should not be synced as the year is not greater
		// than 1968
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Revolver"))
													.with("interpreter__c", "The Beatles")
													.with("year__c", "1966")
													.build());

		// This custom object should BE synced (inserted) as the year is greater
		// than 1968 and the record doesn't exist in the target system
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Amputechture"))
													.with("interpreter__c", "The Mars Volta")
													.with("year__c", "2006")
													.build());

		// This custom object should BE synced (updated) as the year is greater
		// than 1968 and the record exists in the target system
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Physical Graffiti"))
													.with("interpreter__c", "Led Zeppelin")
													.with("year__c", "1975")
													.build());

		MuleEvent event = createCustomObjectFlowA.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage()
															.getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjectsInA.get(i)
									.put("Id", results.get(i)
														.getId());
		}

	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		deleteTestCustomObjectsFromSandBoxA();
		deleteTestCustomObjectsFromSandBoxB();
	}

	private void deleteTestCustomObjectsFromSandBoxA() throws MuleException, Exception {
		// Delete the created custom objects in A
		SubflowInterceptingChainLifecycleWrapper deleteCustomObjectFromAFlow = getSubFlow("deleteCustomObjectFromAFlow");
		deleteCustomObjectFromAFlow.initialise();
		deleteTestEntityFromSandBox(deleteCustomObjectFromAFlow, createdCustomObjectsInA);
	}

	private void deleteTestCustomObjectsFromSandBoxB() throws MuleException, Exception {
		List<Map<String, Object>> createdCustomObjectsInB = new ArrayList<Map<String, Object>>();
		for (final Map<String, Object> c : createdCustomObjectsInA) {
			final Map<String, Object> customObject = invokeRetrieveFlow(retrieveCustomObjectFromBFlow, c);
			if (customObject != null) {
				createdCustomObjectsInB.add(customObject);
			}
		}

		SubflowInterceptingChainLifecycleWrapper deleteCustomObjectFromBFlow = getSubFlow("deleteCustomObjectFromBFlow");
		deleteCustomObjectFromBFlow.initialise();
		deleteTestEntityFromSandBox(deleteCustomObjectFromBFlow, createdCustomObjectsInB);
	}
}
