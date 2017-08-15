package de.novatec.appdynamics.extensions.gemfire.gemfire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class GemfireTestCluster {
    private static final Logger logger = LoggerFactory.getLogger(GemfireTestCluster.class);

    List<Server> servers = Arrays.asList(new Server("server1", 40000, 1099));

    public static void main(String[] args) {
        new GemfireTestCluster().start();
    }

    public void start() {
        for (Server server : servers) {
            server.start();
        }
    }

    public void stop() {
        for (Server server : servers) {
            server.stop();
        }
    }
}
