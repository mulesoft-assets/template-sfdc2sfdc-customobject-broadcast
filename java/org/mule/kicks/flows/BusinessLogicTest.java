package org.mule.kicks.flows;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.munit.runner.functional.FunctionalMunitSuite;
import org.mule.streaming.ConsumerIterator;

public class BusinessLogicTest extends FunctionalMunitSuite {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("mule.env", "test");
	}

	@AfterClass
	public static void afterClass() {
		System.getProperties().remove("mule.env");
	}

	@Before
	public void setUp() {
	}

	@Ignore
	@Test
	@SuppressWarnings("unchecked")
	public void testGatherDataFlow() throws MuleException, Exception {

        ConsumerIterator<Map<String, String>> queryResult = Mockito.mock(ConsumerIterator.class);
		Mockito.when(queryResult.size()).thenReturn(0);

		whenMessageProcessor("query").ofNamespace("sfdc").thenReturn(muleMessageWithPayload(queryResult));

		MuleEvent event = testEvent("");
		runFlow("gatherDataFlow", event);

		verifyCallOfMessageProcessor("query").ofNamespace("sfdc").times(1);

	}
}
