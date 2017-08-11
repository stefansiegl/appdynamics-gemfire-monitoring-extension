package de.novatec.appdynamics.extensions.gemfire;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.novatec.appdynamics.extensions.gemfire.util.Utils.*;
import static de.novatec.appdynamics.extensions.gemfire.util.Contants.*;

/**
 * Created by stefan on 09.08.17.
 */
public class GemfireMonitor extends AManagedMonitor {

    private static final Logger logger = LoggerFactory.getLogger(GemfireMonitor.class);
    private MonitorConfiguration configuration;

    public GemfireMonitor(){
        logger.info(getLogVersion());
    }

    public TaskOutput execute (Map<String, String> map, TaskExecutionContext taskExecutionContext) throws
            TaskExecutionException {
        logger.info(getLogVersion());
        if (logger.isDebugEnabled()) {
            logger.debug("The raw arguments are {}", map);
        }
        try {
            initialize(map);
            configuration.executeTask();
        } catch (Exception ex) {
            if (configuration != null && configuration.getMetricWriter() != null) {
                configuration.getMetricWriter().registerError(ex.getMessage(), ex);
            }
            ex.printStackTrace();
        }
        return null;
    }

    private void initialize (Map<String, String> argsMap) {
        if (configuration == null) {
            MetricWriteHelper metricWriter = MetricWriteHelperFactory.create(this);

            // note that the MonitorConfiguration is some god-class of AppDynamics which holds the execution of tasks.
            // unfortunately you have to provide exactly one runnable when creating the configuration.
            MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|GemFire|", new RunOneTaskPerDefinedServer(),
                    metricWriter);

            final String configFilePath = argsMap.get("config-file");
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem
                    .CONFIG_YML, MonitorConfiguration.ConfItem.HTTP_CLIENT, MonitorConfiguration.ConfItem
                    .EXECUTOR_SERVICE);

            this.configuration = conf;
        }
    }

    /**
     * Spawns a thread per defined server inside the configuration.
     */
    private class RunOneTaskPerDefinedServer implements Runnable {
        public void run () {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null) {
                List<Map> servers = (List) config.get(CONFIG_SERVER);
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        //try {
                            GemFireMonitorTask task = new GemFireMonitorTask.Builder().
                                    serverInformation(server).
                                    metricInformation((Map) config.get("metrics")).
                                    metricWriter(configuration.getMetricWriter()).
                                    metricPrefix(configuration.getMetricPrefix()).
                                    build();

                            configuration.getExecutorService().execute(task);
//                        } catch (IOException e) {
//                            logger.error("Cannot construct JMX uri for {}", convertToString(server.get("displayName")
//                                    , ""));
//                        }
                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    private static String getImplementationVersion () {
        return GemfireMonitor.class.getPackage().getImplementationTitle();
    }

    private String getLogVersion () {
        return "Using GemFire/Apache Geode Monitor Version [" + getImplementationVersion() + "]";
    }

    public static void main (String[] args) throws TaskExecutionException {
        GemfireMonitor gemfireMonitor = new GemfireMonitor();
        Map<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("config-file", "/Users/stefan/Projekte/Daimler/appd/gemfire-monitoring-extension" +
                "" + "" + "/src/main/resources/conf/config.yml");
        gemfireMonitor.execute(argsMap, null);
    }
}
