AppDynamics Monitoring Extension for use with Pivotal GemFire/Apache Geode
==========================================================================

Use Case
--------

This AppDynamics extension allows to retrieve monitoring information form Gemfire or Apache Geode. Gemfire is the 
commercial product based on Apache Geode. 
 
The extension connects to the JMX manager (locators) and retrieve the information. The list of available metrics is 
available here:

https://geode.apache.org/docs/guide/11/managing/management/list_of_mbeans.html


Prerequisites
--------------
The extension is implemented as standard Java-based extension. The connection to JMX is part of the VM, so no 
dependencies are necessary.

Build the extension
-------------------

Install the extension
---------------------

Configuration
-------------
The configuration file