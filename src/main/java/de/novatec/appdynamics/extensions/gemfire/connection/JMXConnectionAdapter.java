package de.novatec.appdynamics.extensions.gemfire.connection;

import com.google.common.base.Strings;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Connection to the JMX server. This class utilizes a retry mechanism in order to allow temporary connection problems
 * to be transparently mitigated.
 *
 * @author Stefan Siegl (sieglst@googlemail.com)
 * @author Stefan Siegl - APM competence group NovaTec Consulting (stefan.siegl@novatec-gmbh.de)
 */
public class JMXConnectionAdapter {

    JMXServiceURL serviceUrl;
    String username;
    String password;
    int port;
    String host;
    MBeanServerConnection connection;
    private JMXConnector connector;

    /**
     * Retrieves all attribute values for the given MBean expression. Note that the mbean expression may have
     * wildcards.
     *
     * @param mbean MBean expression allowing wildcards.
     * @return For each match of the MBean expression, all attribute values are collected and returned.
     * @throws IOException In case of connection problems.
     */
    public Map<String, Map<String, Object>> getAllAttributeValues(String mbean) throws IOException {
        if (!connected()) {
            newConnection();
        }

        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
        try {
            final Set<ObjectInstance> objectInstances = connection.queryMBeans(new ObjectName(mbean), null);
            for (ObjectInstance objectInstance : objectInstances) {
                MBeanInfo info = connection.getMBeanInfo(objectInstance.getObjectName());

                AttributeList attList = connection.getAttributes(objectInstance.getObjectName(), attributeInfosToStringArray(info.getAttributes()));
                Map<String, Object> values = new HashMap<String, Object>();
                for (Object attObject : attList) {
                    Attribute a = (Attribute) attObject;
                    values.put(a.getName(), a.getValue());
                }

                result.put(objectInstance.getObjectName().toString(), values);
            }
            return result;
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return new HashMap<String, Map<String, Object>>();
    }

    /**
     * Close the connection after use.
     */
    public void close() {
        connection = null;
        if (connector != null) {
            try {
                connector.close();
                connector = null;
            } catch (IOException e) {
                // could not close - ignore.
            }
        }
    }

    private String[] attributeInfosToStringArray(MBeanAttributeInfo[] attInfos) {
        String[] attNames = new String[attInfos.length];
        int i = 0;
        for (MBeanAttributeInfo attInfo : attInfos) {
            attNames[i++] = attInfo.getName();
        }
        return attNames;
    }

    private boolean connected () {
        return connector != null;
    }

    private void newConnection() throws IOException {
        final Map<String, Object> env = new HashMap<String, Object>();
        if (!Strings.isNullOrEmpty(username)) {
            env.put(JMXConnector.CREDENTIALS, new String[]{username, password});
            connector = JMXConnectorFactory.connect(serviceUrl, env);
        } else {
            connector = JMXConnectorFactory.connect(serviceUrl);
        }

        connection = connector.getMBeanServerConnection();
    }

    public static class Builder {
        JMXConnectionAdapter adapter = new JMXConnectionAdapter();

        public Builder user(String username) {
            adapter.username = username;
            return this;
        }

        public Builder host(String host) {
            adapter.host = host;
            return this;
        }

        public Builder port(int port) {
            adapter.port = port;
            return this;
        }

        public Builder password(String password) {
            adapter.password = password;
            return this;
        }

        public Builder serviceUrl(JMXServiceURL serviceUrl) {
            adapter.serviceUrl = serviceUrl;
            return this;
        }

        public JMXConnectionAdapter build() throws IOException {
            if (adapter.serviceUrl == null) {
                adapter.serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + adapter.host + ":" + adapter.port + "/jmxrmi");
            }

            return adapter;
        }
    }
}
