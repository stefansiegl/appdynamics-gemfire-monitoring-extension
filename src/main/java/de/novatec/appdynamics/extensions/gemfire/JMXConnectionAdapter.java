package de.novatec.appdynamics.extensions.gemfire;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.management.*;
import javax.management.Attribute;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.print.attribute.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by stefan on 09.08.17.
 */
public class JMXConnectionAdapter {

    JMXServiceURL serviceUrl;
    String username;
    String password;
    int port;
    String host;
    JMXConnector jmxConnector;
    MBeanServerConnection connection;

    private JMXConnectionAdapter() {}

    public static class Builder {
        JMXConnectionAdapter adapter = new JMXConnectionAdapter();

        public Builder user (String username) {
            adapter.username = username;
            return this;
        }

        public Builder host (String host) {
            adapter.host = host;
            return this;
        }

        public Builder port (int port) {
            adapter.port = port;
            return this;
        }

        public Builder password (String password) {
            adapter.password = password;
            return this;
        }

        public Builder serviceUrl (JMXServiceURL serviceUrl) {
            adapter.serviceUrl = serviceUrl;
            return this;
        }

        public JMXConnectionAdapter connect () throws IOException {
            if (adapter.serviceUrl == null) {
                adapter.serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + adapter.host + ":" + adapter.port + "/jmxrmi");
            }

            final Map<String, Object> env = new HashMap<String, Object>();
            if (!Strings.isNullOrEmpty(adapter.username)) {
                env.put(JMXConnector.CREDENTIALS, new String[]{adapter.username, adapter.password});
                adapter.jmxConnector = JMXConnectorFactory.connect(adapter.serviceUrl, env);
            } else {
                adapter.jmxConnector = JMXConnectorFactory.connect(adapter.serviceUrl);
            }
            if (adapter.jmxConnector == null) {
                throw new IOException("Unable to connect to Mbean server");
            }

            adapter.connection = adapter.jmxConnector.getMBeanServerConnection();

            return adapter;
        }
    }

    public Map<String, Map<String, Object>> getAllAttributeValues(String mbean) throws IOException {
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
        } catch (Exception e) {
            throw new IOException("Base Exception is "+e.getMessage());
        }
    }

    private String[] attributeInfosToStringArray (MBeanAttributeInfo[] attInfos) {
        String[] attNames = new String[attInfos.length];
        int i = 0;
        for (MBeanAttributeInfo attInfo : attInfos) {
            attNames[i++] = attInfo.getName();
        }
        return attNames;
    }

    public void close () throws IOException {
        if (jmxConnector != null) {
            jmxConnector.close();
        }
    }

    public Set<ObjectInstance> queryMBeans (JMXConnector jmxConnection, ObjectName objectName) throws IOException {
        MBeanServerConnection connection = jmxConnection.getMBeanServerConnection();
        return connection.queryMBeans(objectName, null);
    }

    public List<String> getReadableAttributeNames (JMXConnector jmxConnection, ObjectInstance instance) throws
            IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        MBeanServerConnection connection = jmxConnection.getMBeanServerConnection();
        List<String> attrNames = Lists.newArrayList();
        MBeanAttributeInfo[] attributes = connection.getMBeanInfo(instance.getObjectName()).getAttributes();
        for (MBeanAttributeInfo attr : attributes) {
            if (attr.isReadable()) {
                attrNames.add(attr.getName());
            }
        }
        return attrNames;
    }

    public Set<Attribute> getAttributes (JMXConnector jmxConnection, ObjectName objectName, String[] strings) throws
            IOException, ReflectionException, InstanceNotFoundException {
        MBeanServerConnection connection = jmxConnection.getMBeanServerConnection();
        AttributeList list = connection.getAttributes(objectName, strings);
        if (list != null) {
            return new HashSet<Attribute>((List)list);
        }
        return Sets.newHashSet();
    }
}
