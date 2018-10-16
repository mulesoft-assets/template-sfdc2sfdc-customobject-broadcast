
# Anypoint Template: Salesforce to Salesforce Custom Object Broadcast	

<!-- Header (start) -->

<!-- Header (end) -->

# License Agreement
This template is subject to the conditions of the <a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>. Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio. 
# Use Case
<!-- Use Case (start) -->
As a Salesforce admin I want to synchronize custom objects between two Salesforce orgs.

This Anypoint template serves as a foundation for setting an online sync of custom objects from one SalesForce instance to another. When there is new custom object or a change in an already existing one, the integration will poll for changes in SalesForce source instance and it will be responsible for updating the custom object on the target org.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this template leverages the Mule batch module.
The batch job is divided in Process and On Complete stages.
During the Input stage the template will go to the SalesForce Org A and query all the existing custom objects that match the filter criteria.
During the Process stage, each Salesforce custom object is filtered depending on if it has an existing matching custom object in the Salesforce Org B.
The last step of the Process stage groups the custom objects and create/update them in Salesforce Org B.
Finally during the On Complete stage the template logs output statistics data into the console.
<!-- Use Case (end) -->

# Considerations
<!-- Default Considerations (start) -->

<!-- Default Considerations (end) -->

<!-- Considerations (start) -->
To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source and destination systems, that must be made for the template to run smoothly. **Failing to do so could lead to unexpected behavior of the template.**

## Create the Custom Object schemas in both organizations <a name="createcustomobjects" />

In order to run the template as is, you'll need to create the custom objects provided in your Salesforce accounts. In order to do so, [please follow the steps documented in SalesForce documentation](http://www.salesforce.com/us/developer/docs/apexcode/Content/apex_qs_customobject.htm).

The custom objects and custom fields created for this application are the following:
1. SalesForce org A
MusicAlbum
	interpreter
	year
	genre
2. SalesForce org B
MusicAlbum
	interpreter
	year
	genre

**Note:** Please, take into account that this sample application uses SalesForce Object Query Language which, when querying for custom objects and fields, requires you to append `__c` to your query. So for example, to query the music albums' interptreters, the query would be this way: `SELECT interpreter__c FROM MusicAlbum__c`.
<!-- Considerations (end) -->



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work:

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>.
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>.

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault 
[ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='Account.Phone, Account.Rating, Account.RecordTypeId, 
Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to 
use a custom field, be sure to append the '__c' after the custom field name. 
Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.









# Run it!
Simple steps to get this template running.
<!-- Run it (start) -->

<!-- Run it (end) -->

## Running On Premises
In this section we help you run this template on your computer.
<!-- Running on premise (start) -->

<!-- Running on premise (end) -->

### Where to Download Anypoint Studio and the Mule Runtime
If you are new to Mule, download this software:

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

**Note:** Anypoint Studio requires JDK 8.
<!-- Where to download (start) -->

<!-- Where to download (end) -->

### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your Anypoint Platform credentials, search for the template, and click Open.
<!-- Importing into Studio (start) -->

<!-- Importing into Studio (end) -->

### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`.
+ Click `Mule Application (configure)`.
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
+ Click `Run`.
<!-- Running on Studio (start) -->

<!-- Running on Studio (end) -->

### Running on Mule Standalone
Update the properties in one of the property files, for example in mule.prod.properties, and run your app with a corresponding environment variable. In this example, use `mule.env=prod`. 


## Running on CloudHub
When creating your application in CloudHub, go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the mule.env value.
<!-- Running on Cloudhub (start) -->
Once your app is all set and started, there is no need to do anything else. Every time a custom object is created or modified, it will be automatically synchronised to Salesforce Org B as long as it has an Email.
<!-- Running on Cloudhub (end) -->

### Deploying a Template in CloudHub
In Studio, right click your project name in Package Explorer and select Anypoint Platform > Deploy on CloudHub.
<!-- Deploying on Cloudhub (start) -->

<!-- Deploying on Cloudhub (end) -->

## Properties to Configure
To use this template, configure properties such as credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
<!-- Application Configuration (start) -->
**Application Configuration**		
		 
+ scheduler.frequency `60000`
+ scheduler.startDelay `0`
+ watermark.default.expression `YESTERDAY`
+ page.size `200`

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
<!-- Application Configuration (end) -->

# API Calls
<!-- API Calls (start) -->
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The template calls to the API can be calculated using the formula:

***1 + X + X / 200***

Being ***X*** the number of Custom Objects to be synchronized on each run. 

The division by ***200*** is because, by default, Custom Objects are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).
<!-- API Calls (end) -->

# Customize It!
This brief guide provides a high level understanding of how this template is built and how you can change it according to your needs. As Mule applications are based on XML files, this page describes the XML files used with this template. More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml<!-- Customize it (start) -->

<!-- Customize it (end) -->

## config.xml
<!-- Default Config XML (start) -->
This file provides the configuration for connectors and configuration properties. Only change this file to make core changes to the connector processing logic. Otherwise, all parameters that can be modified should instead be in a properties file, which is the recommended place to make changes.<!-- Default Config XML (end) -->

<!-- Config XML (start) -->

<!-- Config XML (end) -->

## businessLogic.xml
<!-- Default Business Logic XML (start) -->
Functional aspect of the template is implemented on this XML, directed by one flow that will poll for SalesForce creations/updates. The several message processors constitute four high level actions that fully implement the logic of this template:

1. During the Input stage the template will go to the SalesForce Org A and query all the existing custom objects that match the filter criteria.
2. During the Process stage, each Salesforce custom object is filtered depending on if it has an existing matching custom object in the Salesforce Org B.
3. The last step of the Process stage groups the custom objects and create/update them in Salesforce Org B.
4. Finally during the On Complete stage the template logs output statistics data into the console.<!-- Default Business Logic XML (end) -->

<!-- Business Logic XML (start) -->

<!-- Business Logic XML (end) -->

## endpoints.xml
<!-- Default Endpoints XML (start) -->
This file is conformed by two flows.

The first one we'll call it **scheduler** flow. This one contains the Scheduler endpoint that will periodically trigger **watermarking** flow and then executing the batch job process.

The second one we'll call it **watermarking** flow. This one contains watermarking logic that will be querying Salasforce for updated/created Custom Objects that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each Salasforce query.<!-- Default Endpoints XML (end) -->

<!-- Endpoints XML (start) -->

<!-- Endpoints XML (end) -->

## errorHandling.xml
<!-- Default Error Handling XML (start) -->
This file handles how your integration reacts depending on the different exceptions. This file provides error handling that is referenced by the main flow in the business logic.<!-- Default Error Handling XML (end) -->

<!-- Error Handling XML (start) -->

<!-- Error Handling XML (end) -->

<!-- Extras (start) -->

<!-- Extras (end) -->
