package org.mule.kicks.integration;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.NullPayload;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * @author miguel.oliva
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {

    private static SubflowInterceptingChainLifecycleWrapper checkCustomObjectflow;
    private static List<Map<String, String>> createdCustomObjectsInA = new ArrayList<Map<String, String>>();

    private final Prober workingPollProber = new PollingProber(60000, 1000l);

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        // Flow to retrieve custom objects from target system after syncing
        checkCustomObjectflow = getSubFlow("retrieveCustomObjectFlow");
        checkCustomObjectflow.initialise();

        // Create object in target system to be updated
        final SubflowInterceptingChainLifecycleWrapper flowB = getSubFlow("createCustomObjectFlowB");
        flowB.initialise();

        final List<Map<String, String>> createdCustomObjectsInB = new ArrayList<Map<String, String>>();
        // This custom object should BE synced (updated) as the year is greater than 1968 and the record exists in the target system
        createdCustomObjectsInB.add(aCustomObject()
                                        .withProperty("Name", "Physical Graffiti")
                                        .withProperty("interpreter__c", "Lead Zep")
                                        .withProperty("genre__c", "Hard Rock")
                                        .build());

        flowB.process(getTestEvent(createdCustomObjectsInB, MessageExchangePattern.REQUEST_RESPONSE));

        // Create custom objects in source system to be or not to be synced
        final SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createCustomObjectFlowA");
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

        final MuleEvent event = flow.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
        final List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
        for (int i = 0; i < results.size(); i++) {
            createdCustomObjectsInA.get(i).put("Id", results.get(i).getId());
        }
    }

    @After
    public void tearDown() throws Exception {
        stopSchedulers();

        // Delete the created custom objects in A
        SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteCustomObjectFromAFlow");
        flow.initialise();

        final List<String> idList = new ArrayList<String>();
        for (final Map<String, String> c : createdCustomObjectsInA) {
            idList.add(c.get("Id"));
        }
        flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

        // Delete the created custom objects in B
        flow = getSubFlow("deleteCustomObjectFromBFlow");
        flow.initialise();

        idList.clear();
        for (final Map<String, String> c : createdCustomObjectsInA) {
            final Map<String, String> customObject = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, c);
            if (customObject != null) {
                idList.add(customObject.get("Id"));
            }
        }
        flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
    }

    @Test
    public void testMainFlow() throws Exception {
        workingPollProber.check(new AssertionProbe() {
            @Override
            public void assertSatisfied() throws Exception {
                // Assert first object was not synced
                assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(0)));

                // Assert second object was not synced
                assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(1)));

                // Assert third object was created in target system
                Map<String, String> payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(2));
                assertEquals("The custom object should have been sync", createdCustomObjectsInA.get(2).get("Name"), payload.get("Name"));

                // Assert fourth object was updated in target system
                final Map<String, String> fourthCustomObject = createdCustomObjectsInA.get(3);
                payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, fourthCustomObject);
                assertEquals("The custom object should have been sync (Name)", fourthCustomObject.get("Name"), payload.get("Name"));
                assertEquals("The custom object should have been sync (interpreter__c)", fourthCustomObject.get("interpreter__c"), payload.get("interpreter__c"));
            }
        });
    }

    private void stopSchedulers() throws MuleException {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.flowPollingSchedulers("upsertCustomObjectFromAToB"));

        for (final Scheduler scheduler : schedulers) {
            scheduler.stop();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeRetrieveCustomObjectFlow(final SubflowInterceptingChainLifecycleWrapper flow, final Map<String, String> customObject) throws Exception {
        final Map<String, String> customObjectMap = new HashMap<String, String>();

        customObjectMap.put("Name", customObject.get("Name"));

        final MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
        final Object payload = event.getMessage().getPayload();
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

        private final Map<String, String> customObject = new HashMap<String, String>();

        public CustomObjectBuilder withProperty(final String key, final String value) {
            customObject.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return customObject;
        }

    }

}
