package de.novatec.appdynamics.extensions.gemfire;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import de.novatec.appdynamics.extensions.gemfire.gemfire.GemfireTestCluster;
import de.novatec.appdynamics.extensions.gemfire.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class GemFireMonitorTaskTest {

    // we use this to parse the configuration
    private MonitorConfiguration configuration;
    private GemfireTestCluster testCluster;
    @Mock
    private MetricWriteHelper spiedWriter;

    @Before
    public void initializeLocalGemFireCluster() {
        testCluster = new GemfireTestCluster();
        testCluster.start();
    }

    @Before
    public void readConfiguration() {
        // only used to parse the configuration.
        configuration = new MonitorConfiguration("blub", new Runnable() {
            public void run() {
                // ignore
            }
        }, new MetricWriteHelper() {
            // ignore
        });
        configuration.setConfigYml("src/test/resources/config.yml");
    }

    @After
    public void tearDown() {
        testCluster.stop();
    }

    @Test
    public void runExtractor() {
        GemFireMonitorTask task = new GemFireMonitorTask.Builder()
                .metricInformation((Map) configuration.getConfigYml().get(Constants.CONFIG_METRICS))
                .serverInformation((Map) ((List) configuration.getConfigYml().get(Constants.CONFIG_SERVER)).get(0))
                .metricPrefix(configuration.getMetricPrefix())
                .metricWriter(spiedWriter)
                .build();

        task.run();

        Mockito.verify(spiedWriter).printMetric("PREFIX|GemFire1|system|System|LocatorCount", new BigDecimal(0), "AVG.AVG.COL");
        Mockito.verify(spiedWriter).printMetric("PREFIX|GemFire1|system|System|MemberCount", new BigDecimal(1), "AVG.AVG.COL");
        Mockito.verify(spiedWriter).printMetric("PREFIX|GemFire1|servers|ServerDetails|server1|VisibleNodes", new BigDecimal(0), "AVG.AVG.COL");
        Mockito.verifyNoMoreInteractions(spiedWriter);
    }

}
