package de.novatec.appdynamics.extensions.gemfire;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeList;
import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class does the heavy lifting of reading the configuration, querying the JMX Beans and writing it to AppDynamics.
 */
public class GemFireMonitorTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GemFireMonitorTask.class);

    private String serverName;
    private Map serverInformation;
    private Map metricInformation;
    private MetricWriteHelper metricWriter;
    private String metricPrefix;

    private GemFireMonitorTask () {
    }

    static class Builder {
        GemFireMonitorTask task = new GemFireMonitorTask();

        public Builder serverInformation (Map serverInfo) {
            task.serverInformation = serverInfo;
            return this;
        }

        public Builder metricInformation (Map metricInformation) {
            task.metricInformation = metricInformation;
            return this;
        }

        public Builder metricWriter (MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        public Builder metricPrefix (String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        public GemFireMonitorTask build() {
            task.serverName = (String) task.serverInformation.get("displayName");
            return task;
        }
    }

    /**
     * Executes the GemFire extraction logic.
     */
    public void run() {

        JMXConnectionAdapter con = null;
        try {
            con = connect(serverInformation);

            // we iterate over all given configurations
            for (Object metricSectionsObject : metricInformation.entrySet()) {
                Map.Entry<String, Map> metricSection = (Map.Entry<String, Map>) metricSectionsObject;
                String metricSectionName = metricSection.getKey();
                Map sectionEntries = metricSection.getValue();

                String mbean = (String) sectionEntries.get("mbean");

                Map<String, Map<String, Object>> attributeValues = null;
                try {
                    attributeValues = con.getAllAttributeValues(mbean);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (String objectName : attributeValues.keySet()) {
                    Map<String, Object> values = attributeValues.get(objectName);

                    Collection c = ((Map) sectionEntries.get("metrics")).values();
                    for (Object metricObject : ((Map) sectionEntries.get("metrics")).entrySet()) {
                        Map.Entry<String, Map> metricDetails = (Map.Entry<String, Map>) metricObject;
                        String mbeanAttribute = metricDetails.getKey();
                        String appDynamicsName = mbeanAttribute;
                        int scale = 1;

                        if (metricDetails.getValue() != null) {
                            Map optionalDetails = (Map) metricDetails.getValue();
                            if (optionalDetails.get("appDynamicsName") != null)
                                appDynamicsName = (String) optionalDetails.get("appDynamicsName");
                            if (optionalDetails.get("scale") != null)
                                scale = (Integer) optionalDetails.get("scale");
                        }

                        BigDecimal value = convert(values.get(mbeanAttribute), scale);

                        //TODO: Define the metric type with the configuration
                        String metricType = "AVG.AVG.COL";

                        metricWriter.printMetric(createMetricPath(metricSectionName, objectName, appDynamicsName), value, metricType);
                        //System.out.printf("Read %s %s %s %s %s %s %s %s \n", serverName, mbean, objectName, metricSectionName, mbeanAttribute, appDynamicsName, scale, value);
                        System.out.printf("%s %s %s  \n", createMetricPath(metricSectionName, objectName, appDynamicsName), value, metricType);

                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in Gemfire/Geode Monitor for Server {}", serverName, e);
        } finally {
            logger.debug("Finished Running Gemfire/Geode Monitor for server {}", serverName);
        }




        // we iterate over all given configurations
        for (Object metricSectionsObject : metricInformation.entrySet()) {
            Map.Entry<String, Map> metricSection = (Map.Entry<String, Map>) metricSectionsObject;
            String metricSectionName = metricSection.getKey();
            Map sectionEntries = metricSection.getValue();

            String mbean = (String) sectionEntries.get("mbean");

            Map<String, Map<String, Object>> attributeValues = null;
            try {
                attributeValues = con.getAllAttributeValues(mbean);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String objectName : attributeValues.keySet()) {
                Map<String, Object> values = attributeValues.get(objectName);

                Collection c = ((Map) sectionEntries.get("metrics")).values();
                for (Object metricObject : ((Map) sectionEntries.get("metrics")).entrySet()) {
                    Map.Entry<String, Map> metricDetails = (Map.Entry<String, Map>) metricObject;
                    String mbeanAttribute = metricDetails.getKey();
                    String appDynamicsName = mbeanAttribute;
                    int scale = 1;

                    if (metricDetails.getValue() != null) {
                        Map optionalDetails = (Map) metricDetails.getValue();
                        if (optionalDetails.get("appDynamicsName") != null)
                            appDynamicsName = (String) optionalDetails.get("appDynamicsName");
                        if (optionalDetails.get("scale") != null)
                            scale = (Integer) optionalDetails.get("scale");
                    }

                    BigDecimal value = convert(values.get(mbeanAttribute), scale);

                    String metricType = "AVG.AVG.COL";

                    metricWriter.printMetric(createMetricPath(metricSectionName, extractObjectName(objectName), appDynamicsName), value, metricType);
              //  public void printMetric(String metricPath, BigDecimal value, String metricType) {
                    // read values:
                    // System.out.printf("Read %s %s %s %s %s %s %s %s \n", serverName, mbean, objectName, metricSectionName, mbeanAttribute, appDynamicsName, scale, value);
                    System.out.printf("%s %s %s \n", createMetricPath(metricSectionName, extractObjectName(objectName), appDynamicsName), value, metricType);

                }
            }
        }

        System.out.println("finished");
    }

    private String extractObjectName (String objectName) {
        // GemFire:service=System,type=Distributed
        if (objectName.contains("service=System")) {
            return "System";
        }

        // GemFire:type=Member,member=locator1
        if (objectName.contains("type=Member")) {
            return objectName.split("\\=")[2];
        }

        throw new RuntimeException("Unknown objectname pattern, please provide a mapping to an appdynamics name. "+objectName);
    }

    /**
     * Converts the given numerical value to BigDecimal and applies the scaling factor.
     * @param o the numerical object.
     * @param scale the scaling factor.
     * @return BigDecimal of the value.
     */
    private BigDecimal convert (Object o, int scale) {
        if (o instanceof Long) {
            return BigDecimal.valueOf(((Long) o) * scale);
        } else if (o instanceof Integer) {
            return BigDecimal.valueOf(new Long (((Integer) o) * scale));
        } else if (o instanceof Float) {
            Float f = (Float) o;
            float val = f.floatValue() * scale;
            return BigDecimal.valueOf(new Float(val).longValue());
        } else if (o instanceof Double) {
            Double d = (Double) o;
            double val = d.doubleValue() * scale;
            return BigDecimal.valueOf(new Double(val).longValue());
        }
        throw new RuntimeException("Missing conversion for "+o.getClass()+". Note that AppD cannot handle non numbers, so strings cannot be written.");
    }

    private String createMetricPath (String metricSectionName, String objectName, String appDynamicsName) {
        return metricPrefix + "|" + serverName + "|" + metricSectionName + "|" + objectName + "|" + appDynamicsName;
    }

    private JMXConnectionAdapter connect (Map serverInformation) throws IOException {
        return new JMXConnectionAdapter.Builder().
                host((String) serverInformation.get("host")).
                port((Integer) serverInformation.get("port")).
                user((String) serverInformation.get("username")).
                password((String) serverInformation.get("password")).
                connect();
    }
}
