package org.mule.kicks.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mule.kicks.builders.SfdcObjectBuilder.aCustomObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.NullPayload;

import com.mulesoft.module.batch.api.BatchJobInstance;
import com.mulesoft.module.batch.api.notification.BatchNotification;
import com.mulesoft.module.batch.api.notification.BatchNotificationListener;
import com.mulesoft.module.batch.engine.BatchJobInstanceAdapter;
import com.mulesoft.module.batch.engine.BatchJobInstanceStore;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule Kick that make calls to external systems.
 * 
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkCustomObjectflow;
	private static List<Map<String, Object>> createdCustomObjectsInA = new ArrayList<Map<String, Object>>();

	private final Prober workingPollProber = new PollingProber(60000, 1000l);

	protected static final int TIMEOUT = 60;

	private Prober prober;
	protected Boolean failed;
	protected BatchJobInstanceStore jobInstanceStore;

	protected class BatchWaitListener implements BatchNotificationListener {

		public synchronized void onNotification(ServerNotification notification) {
			final int action = notification.getAction();

			if (action == BatchNotification.JOB_SUCCESSFUL || action == BatchNotification.JOB_STOPPED) {
				failed = false;
			} else if (action == BatchNotification.JOB_PROCESS_RECORDS_FAILED || action == BatchNotification.LOAD_PHASE_FAILED || action == BatchNotification.INPUT_PHASE_FAILED
					|| action == BatchNotification.ON_COMPLETE_FAILED) {

				failed = true;
			}
		}
	}

	@BeforeClass
	public static void setTestProperties() {
		System.setProperty("page.size", "1000");

		// Set the frequency between polls to 10 seconds
		System.setProperty("poll.frequencyMillis", "10000");

		// Set the poll starting delay to 20 seconds
		System.setProperty("poll.startDelayMillis", "20000");

		// Setting Default Watermark Expression to query SFDC with LastModifiedDate greater than ten seconds before current time
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		stopSchedulers();

		failed = null;
		jobInstanceStore = muleContext.getRegistry()
										.lookupObject(BatchJobInstanceStore.class);
		muleContext.registerListener(new BatchWaitListener());

		// Flow to retrieve custom objects from target system after syncing
		checkCustomObjectflow = getSubFlow("retrieveCustomObjectFlow");
		checkCustomObjectflow.initialise();

		// Create object in target system to be updated
		final SubflowInterceptingChainLifecycleWrapper flowB = getSubFlow("createCustomObjectFlowB");
		flowB.initialise();

		final List<Map<String, Object>> createdCustomObjectsInB = new ArrayList<Map<String, Object>>();
		// This custom object should BE synced (updated) as the year is greater than 1968 and the record exists in the target system
		createdCustomObjectsInB.add(aCustomObject().with("Name", buildUniqueName("Physical Graffiti"))
													.with("interpreter__c", "Lead Zep")
													.with("genre__c", "Hard Rock")
													.build());

		flowB.process(getTestEvent(createdCustomObjectsInB, MessageExchangePattern.REQUEST_RESPONSE));

		// Create custom objects in source system to be or not to be synced
		final SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createCustomObjectFlowA");
		flow.initialise();

		// This custom object should not be synced as the year is not greater than 1968
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Are You Experienced"))
													.with("interpreter__c", "Jimi Hendrix")
													.with("year__c", "1967")
													.build());

		// This custom object should not be synced as the year is not greater than 1968
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Revolver"))
													.with("interpreter__c", "The Beatles")
													.with("year__c", "1966")
													.build());

		// This custom object should BE synced (inserted) as the year is greater than 1968 and the record doesn't exist in the target system
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Amputechture"))
													.with("interpreter__c", "The Mars Volta")
													.with("year__c", "2006")
													.build());

		// This custom object should BE synced (updated) as the year is greater than 1968 and the record exists in the target system
		createdCustomObjectsInA.add(aCustomObject().with("Name", buildUniqueName("Physical Graffiti"))
													.with("interpreter__c", "Led Zeppelin")
													.with("year__c", "1975")
													.build());

		final MuleEvent event = flow.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
		final List<SaveResult> results = (List<SaveResult>) event.getMessage()
																	.getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjectsInA.get(i)
									.put("Id", results.get(i)
														.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		failed = null;

		// Delete the created custom objects in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteCustomObjectFromAFlow");
		flow.initialise();

		final List<String> idList = new ArrayList<String>();
		for (final Map<String, Object> c : createdCustomObjectsInA) {
			idList.add(String.valueOf(c.get("Id")));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created custom objects in B
		flow = getSubFlow("deleteCustomObjectFromBFlow");
		flow.initialise();

		idList.clear();
		for (final Map<String, Object> c : createdCustomObjectsInA) {
			final Map<String, String> customObject = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, c);
			if (customObject != null) {
				idList.add(customObject.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("batchJobTriggererFlow");
		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));
		BatchJobInstance batchJobInstance = (BatchJobInstance) event.getMessage()
																	.getPayload();

		awaitJobTermination();

		assertTrue("Batch job was not successful", wasJobSuccessful());

		batchJobInstance = getUpdatedInstance(batchJobInstance);

		// startSchedulers();

		workingPollProber.check(new AssertionProbe() {
			@Override
			public void assertSatisfied() throws Exception {
				// Assert first object was not synced
				assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(0)));

				// Assert second object was not synced
				assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(1)));

				// Assert third object was created in target system
				Map<String, String> payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(2));
				assertEquals("The custom object should have been sync", createdCustomObjectsInA.get(2)
																								.get("Name"), payload.get("Name"));

				// Assert fourth object was updated in target system
				final Map<String, Object> fourthCustomObject = createdCustomObjectsInA.get(3);
				payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, fourthCustomObject);
				assertEquals("The custom object should have been sync (Name)", fourthCustomObject.get("Name"), payload.get("Name"));
				assertEquals("The custom object should have been sync (interpreter__c)", fourthCustomObject.get("interpreter__c"), payload.get("interpreter__c"));
			}
		});
	}

	private void stopSchedulers() throws MuleException {
		final Collection<Scheduler> schedulers = muleContext.getRegistry()
															.lookupScheduler(Schedulers.allPollSchedulers());

		for (final Scheduler scheduler : schedulers) {
			scheduler.stop();
		}
	}

	private void scheduleSchedulers() throws Exception {
		final Collection<Scheduler> schedulers = muleContext.getRegistry()
															.lookupScheduler(Schedulers.allPollSchedulers());

		for (final Scheduler scheduler : schedulers) {
			scheduler.schedule();
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveCustomObjectFlow(final SubflowInterceptingChainLifecycleWrapper flow, final Map<String, Object> c) throws Exception {
		final Map<String, Object> customObjectMap = aCustomObject().with("Name", c.get("Name"))
																	.build();

		final MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
		final Object payload = event.getMessage()
									.getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	private String buildUniqueName(String name) {
		String kickName = "customobjectoneway";
		String timeStamp = new Long(new Date().getTime()).toString();

		StringBuilder builder = new StringBuilder();
		builder.append(name);
		builder.append(kickName);
		builder.append(timeStamp);

		return builder.toString();
	}

	protected void awaitJobTermination() throws Exception {
		this.awaitJobTermination(TIMEOUT);
	}

	protected void awaitJobTermination(long timeoutSecs) throws Exception {
		this.prober = new PollingProber(timeoutSecs * 1000, 500);
		this.prober.check(new Probe() {

			@Override
			public boolean isSatisfied() {
				return failed != null;
			}

			@Override
			public String describeFailure() {
				return "batch job timed out";
			}
		});
	}

	protected boolean wasJobSuccessful() {
		return this.failed != null ? !this.failed : false;
	}

	protected BatchJobInstanceAdapter getUpdatedInstance(BatchJobInstance jobInstance) {
		return this.jobInstanceStore.getJobInstance(jobInstance.getOwnerJobName(), jobInstance.getId());
	}
}
