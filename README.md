# Mule Kick: SFDC to SFDC Automatic Contact Sync

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
As a Salesforce admin I want to syncronize contacts between two Salesfoce orgs.

This Kick (template) should serve as a foundation for setting an online sync of Contacts from one SalesForce instance to another. Everytime there is a new Contact or a change in an already existing one, SFDC Streaming API will notify this integration that will be responsible for updating the Contact on the target org.

Requirements have been set not only to be used as examples, but also to stablish starting point to adapt your integration to your requirements.

# Run it!

## Using SFDC Streaming API <a name="runit"/>

Before running this Kick you have to configure your SFDC Instance to notify the integration every time a Contact is created or updated, this is accomplished by creating a [Topic](http://wiki.developerforce.com/page/Getting_Started_with_the_Force.com_Streaming_API).

The SOQL Query for the topic has to be `SELECT Id,Department,Email,FirstName,LastName,MailingCity,MailingCountry,MobilePhone,Phone,Title FROM Contact` and this will be the fields updated on the SFDC Instance B upon a create/update on Instance A.

**Note:** SFDC will only react to changes on contacts if the fields on the query defined before are modified. If you want to have an update when any change on a Contact happens, that could be done by setting to `All` the property `NotifyForFields` of the topic to be used.


## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, there is no need to do anything else. Every time a Contact is created or modified, it will be automatically synchronised to SFDC Org B as long as it has an Email.


## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

Once your app is all set and started, there is no need to do anything else. Every time a Contact is created or modified, it will be automatically synchronised to SFDC Org B as long as it has an Email.


## Properties to be configured (With examples) <a name="propertiestobeconfigured"/>

In order to use this Mule Kick you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:

### Application configuration
+ http.port `9090` 

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/26.0`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/26.0`

#### Topic created on SFDC Instance A
+ sfdc.a.topic `/contactstopic`

It is important to put the `/` before the name of the topic like showed above (The example is about a topic just named *contactstopic*).



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
2. Filtering of Contacts that must have an Email and the Mailing Country have to be either **US**, **U.S.** or  **United States**. This is the point where you can configure your own filtering criteria.
3. Checking if Contact already exists in target instance by EMail. Step needed to add the extisting ID to update the Contact.
4. Update or create of the Contact in target instance.


## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions. 