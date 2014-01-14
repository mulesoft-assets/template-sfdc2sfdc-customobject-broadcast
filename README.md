# Mule Kick: SFDC to SFDC Automatic Custom Object Sync

+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [Running on CloudHub](#runoncloudhub)
    * [Running on premise](#runonopremise)
    * [Properties to be configured](#propertiestobeconfigured)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)


# Use Case <a name="usecase"/>
As a Salesforce admin I want to syncronize custom objects between two Salesfoce orgs.

This Kick (template) should serve as a foundation for setting an online sync of custom objects from one SalesForce instance to another. Everytime there is a new custom object or a change in an already existing one, SFDC Streaming API will notify this integration that will be responsible for updating the custom object on the target org.

Requirements have been set not only to be used as examples, but also to stablish starting point to adapt your integration to your requirements.

# Run it!

Simple steps to get SFDC to SFDC Custom Objects Sync running.

## Create the Custom Object schemas in both organizations <a name="createcustomobjects" />

In order to run the Kick as is, you'll need to create the custom objects provided in your Salesforce accounts. In order to do so, [please follow the steps documented in SalesForce documentation](http://www.salesforce.com/us/developer/docs/apexcode/Content/apex_qs_customobject.htm).

The custom objects and custom fields created for this application are the following:
1. SalesForce org A
MusicAlbum
	interpreter
	year
2. SalesForce org B
MusicAlbum
	interpreter
	genre

**Note:** Please, take into account that this sample application uses SalesForce Object Query Language which, when querying for custom objects and fields, requires you to append `__c` to your query. So for example, to query the music albums' interptreters, the query would be this way: `SELECT interpreter__c FROM MusicAlbum__c`.

## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, there is no need to do anything else. Every time a custom object is created or modified, it will be automatically synchronised to SFDC Org B as long as it has an Email.


## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

Once your app is all set and started, there is no need to do anything else. The application will poll SalesForce to know if there are any newly created or updated objects and synchronice them.

## Properties to be configured (With examples) <a name="propertiestobeconfigured"/>

In order to use this Mule Kick you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:

### Application configuration
+ http.port `9090` 

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/28.0`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/28.0`

It is important to put the `/` before the name of the topic like showed above (The example is about a topic just named *custom objectstopic*).



# Customize It!<a name="customizeit"/>

This brief guide intends to give a high level idea of how this Kick is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organized by describing all the XML that conform the Kick.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
Configuration for Connectors and [Properties Place Holders](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. **Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.


## businessLogic.xml<a name="businesslogicxml"/>
Functional aspect of the kick is implemented on this XML, directed by one flow that will react upon notifications received from SalesForce Streaming API. The severeal message processors constitute four high level actions that fully implement the logic of this Kick:

1. Suscribe-topic message processor listeting to event from the topic configured in the SalesForce instance.
2. Filtering of custom objects that must have an Email and the Mailing Country have to be either **US**, **U.S.** or  **United States**. This is the point where you can configure your own filtering criteria.
3. Checking if custom object already exists in target instance by EMail. Step needed to add the extisting ID to update the custom object.
4. Update or create of the custom object in target instance.


## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions. 