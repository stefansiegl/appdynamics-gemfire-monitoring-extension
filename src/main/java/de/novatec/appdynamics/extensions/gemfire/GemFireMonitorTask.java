package de.novatec.appdynamics.extensions.gemfire;

import com.appdynamics.extensions.util.MetricWriteHelper;
import de.novatec.appdynamics.extensions.gemfire.connection.JMXConnectionAdapter;
import de.novatec.appdynamics.extensions.gemfire.util.AppDTypeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import static de.novatec.appdynamics.extensions.gemfire.util.Constants.*;

/**
 * This class does the heavy lifting of reading the configuration, querying the JMX Beans and writing it to AppDynamics.
 *
 * @author Stefan Siegl (sieglst@googlemail.com)
 * @author Stefan Siegl - APM competence group NovaTec Consulting (stefan.siegl@novatec-gmbh.de)
 */
public class GemFireMonitorTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GemFireMonitorTask.class);

    private String serverName;
    private Map serverInformation;
    private Map metricInformation;
    private MetricWriteHelper metricWriter;
    private String metricPrefix;
    private AppDTypeFormatter typeFormatter = new AppDTypeFormatter();

    private GemFireMonitorTask() {
    }

    /**
     * Executes the GemFire extraction logic.
     */
    public void run() {
        JMXConnectionAdapter con = null;
        try {
            con = connect(serverInformation);
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully connected to JMXServer "+serverName);
            }

            // A bit ugly without any type safety, but I decided to keep with the other AppDynamics extensions and
            // simply read the yaml configuration.
            for (Object metricSectionsObject : metricInformation.entrySet()) {
                Map.Entry<String, Map> metricSection = (Map.Entry<String, Map>) metricSectionsObject;
                String metricSectionName = metricSection.getKey();
                Map sectionEntries = metricSection.getValue();
                String mbean = (String) sectionEntries.get(CONFIG_MBEAN);
                logger.debug("Starting section {} using mbean expression {}", metricSectionName, mbean);

                // read all JMX values for everything that matches the mbean expression. This is good enough for us
                // as we will want to have more than 50% of the JMX attributes anyway.
                Map<String, Map<String, Object>> attributeValues = null;
                try {
                    attributeValues = con.getAllAttributeValues(mbean);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Read jmx values from given mbean. ", attributeValues);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (String objectName : attributeValues.keySet()) {
                    Map<String, Object> values = attributeValues.get(objectName);

                    logger.debug("Reading data for objectname: {}", objectName);

                    Collection c = ((Map) sectionEntries.get(CONFIG_METRICS)).values();
                    for (Object metricObject : ((Map) sectionEntries.get(CONFIG_METRICS)).entrySet()) {
                        Map.Entry<String, Map> metricDetails = (Map.Entry<String, Map>) metricObject;
                        String mbeanAttribute = metricDetails.getKey();
                        String appDynamicsName = mbeanAttribute;
                        int scale = 1;

                        if (metricDetails.getValue() != null) {
                            Map optionalDetails = (Map) metricDetails.getValue();
                            if (optionalDetails.get(CONFIG_APPDYNAMICS_NAME) != null)
                                appDynamicsName = (String) optionalDetails.get(CONFIG_APPDYNAMICS_NAME);
                            if (optionalDetails.get(CONFIG_SCALE) != null)
                                scale = (Integer) optionalDetails.get(CONFIG_SCALE);
                        }


                        logger.debug("Reading detail data for {}", mbeanAttribute);

                        Object attValue = values.get(mbeanAttribute);
                        if (attValue == null) {
                            logger.warn("No value. The JMXBean {} does not provide the attribute {}", objectName, mbeanAttribute);
                            continue;
                        }


                        BigDecimal value =  typeFormatter.convert(attValue, scale);

                        // Right now we use the same aggregation format for all metrics. If different formats are
                        // in fact necessary, this information should be moved to the configuration part.
                        String metricType = "AVG.AVG.COL";
                        String metricPath = createMetricPath(metricSectionName, objectName, appDynamicsName);

                        logger.debug("Writing: metricpath: {} value: {} metrictype: {}", metricPath, value, metricType);

                        metricWriter.printMetric(metricPath, value, metricType);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in Gemfire/Geode Monitor for Server {}", serverName, e);
        } finally {
            logger.debug("Finished Running Gemfire/Geode Monitor for server {}", serverName);
        }
    }

    private String createMetricPath(String metricSectionName, String objectName, String appDynamicsName) {
        return metricPrefix + "|" + serverName + "|" + metricSectionName + "|" + typeFormatter.formatObjectName(objectName) + "|" + appDynamicsName;
    }

    private JMXConnectionAdapter connect(Map serverInformation) throws IOException {
        return new JMXConnectionAdapter.Builder().
                host((String) serverInformation.get(CONFIG_SERVER_HOST)).
                port((Integer) serverInformation.get(CONFIG_SERVER_PORT)).
                user((String) serverInformation.get(CONFIG_SERVER_USER)).
                password((String) serverInformation.get(CONFIG_SERVER_PASSWORD)).
                connect();
    }

    static class Builder {
        GemFireMonitorTask task = new GemFireMonitorTask();

        Builder serverInformation(Map serverInfo) {
            task.serverInformation = serverInfo;
            return this;
        }

        Builder metricInformation(Map metricInformation) {
            task.metricInformation = metricInformation;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        GemFireMonitorTask build() {
            task.serverName = (String) task.serverInformation.get(CONFIG_SERVER_DISPLAYNAME);
            return task;
        }
    }
}
