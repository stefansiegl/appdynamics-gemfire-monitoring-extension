AppDynamics Monitoring Extension for use with Pivotal GemFire/Apache Geode
==========================================================================

Use Case
--------

This AppDynamics extension allows to retrieve monitoring information form Gemfire or Apache Geode. Gemfire is the 
commercial product based on Apache Geode. 
 
The extension connects to the JMX manager (locators) and retrieve the information. The list of available metrics is 
available here:

https://geode.apache.org/docs/guide/11/managing/management/list_of_mbeans.html

Please note that this extension is quickly built for what I needed it for. I did not test it to monitor multiple GemFire 
clusters at the same time (although the code should be capable of doing that)

Prerequisites
--------------
None. The extension is implemented as standard Java-based extension. The connection to JMX is part of the VM, so no 
dependencies are necessary.

Build the extension
-------------------
Run maven clean install. You find the extension at target/GemFireMonitor-x.zip

Install the extension
---------------------


Configuration
-------------
The configuration file defines which metrics you want to capture, where your GemFire cluster are running and
what credentials you need in order to connect.

License
-------
