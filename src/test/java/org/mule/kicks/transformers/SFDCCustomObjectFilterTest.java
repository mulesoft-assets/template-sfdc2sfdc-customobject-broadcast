package org.mule.kicks.transformers;


import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.NullPayload;


@RunWith(MockitoJUnitRunner.class)
public class SFDCCustomObjectFilterTest {

	private static final String CUSTOM_OBJECT_IN_COMPANY_B = "customObjectInB";
	
	@Mock
	private MuleContext muleContext;

	@Test
	public void testCustomObjectNotInOrgB() throws TransformerException {
		
		MuleMessage message = new DefaultMuleMessage(null, muleContext);
		message.setPayload(getCustomObjectFromSFDCA());
		message.setInvocationProperty(CUSTOM_OBJECT_IN_COMPANY_B, NullPayload.getInstance());
		
		SFDCCustomObjectFilter transformer = new SFDCCustomObjectFilter();
		
		MuleMessage resultingCustomObject = (MuleMessage) transformer.transform(message);
		
		Map<String,String> expectedCustomObject = getCustomObjectFromSFDCA();
		expectedCustomObject.remove("LastModifiedDate");
		
		Assert.assertEquals(expectedCustomObject ,resultingCustomObject.getPayload());

	}
	
	@Test
	public void testSyncCustomObject() throws TransformerException {
		
		MuleMessage message = new DefaultMuleMessage(null, muleContext);
		message.setPayload(getCustomObjectFromSFDCA());
		Map<String,String> contactInB = new HashMap<String,String>();
		contactInB.put("Id", "I000032300ESE");
		contactInB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");
		message.setInvocationProperty(CUSTOM_OBJECT_IN_COMPANY_B, contactInB);
		
		SFDCCustomObjectFilter transformer = new SFDCCustomObjectFilter();
		
		MuleMessage resultingCustomObject = (MuleMessage) transformer.transform(message);
		
		Map<String,String> expectedCustomObject = getCustomObjectFromSFDCA();
		expectedCustomObject.remove("LastModifiedDate");
		expectedCustomObject.put("Id", "I000032300ESE");

		Assert.assertEquals(expectedCustomObject, resultingCustomObject.getPayload());

	}
	
	@Test
	public void testDoNotSyncCustomObject() throws TransformerException {
		
		MuleMessage message = new DefaultMuleMessage(null, muleContext);
		message.setPayload(getCustomObjectFromSFDCA());
		Map<String,String> customObjectInB = new HashMap<String,String>();
		customObjectInB.put("Id", "I000032300ESE");
		customObjectInB.put("LastModifiedDate", "2013-12-11T22:15:33.001Z");
		message.setInvocationProperty(CUSTOM_OBJECT_IN_COMPANY_B, customObjectInB);
		
		SFDCCustomObjectFilter transformer = new SFDCCustomObjectFilter();
		
		MuleMessage resultingCustomObject = (MuleMessage) transformer.transform(message);
		
		Assert.assertEquals(NullPayload.getInstance(), resultingCustomObject.getPayload());

	}
	
	private Map<String,String> getCustomObjectFromSFDCA () {
		
		Map<String,String> customObjectFromSFDCA = new HashMap<String,String>();
		customObjectFromSFDCA.put("Id", "I0000323AE754F");
		customObjectFromSFDCA.put("LastModifiedDate", "2013-12-10T22:15:33.001Z");
		
		return customObjectFromSFDCA;
	}
	
}
