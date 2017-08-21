package de.novatec.appdynamics.extensions.gemfire.gemfire;

import org.apache.geode.distributed.AbstractLauncher;
import org.apache.geode.distributed.ServerLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Represents a Gemfire cluster node.
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private String name;
    private int port;
    private int jmxPort;
    private ServerLauncher launcher;

    public Server(String name, int port, int jmxPort) {
        this.name = name;
        this.port = port;
        this.jmxPort = jmxPort;

        File workingDir = new File("target/" + name).getAbsoluteFile();
        workingDir.mkdirs();

        ServerLauncher serverLauncher = new ServerLauncher.Builder()
                .setMemberName(name)
                .setServerPort(port)
                .set("jmx-manager", "true")
                .set("jmx-manager-start", "true")
                .set("jmx-manager-port", "" + jmxPort)
                .set("deploy-working-dir", workingDir.toString())
                .set("log-file", workingDir.toString() + "/" + name + ".log")
                .setWorkingDirectory(workingDir.toString())
                .build();

        launcher = serverLauncher;
    }

    public void start() {
        ServerLauncher.ServerState state = launcher.start();
        if (!AbstractLauncher.Status.ONLINE.equals(state.getStatus())) {
            logger.warn("Could not start server, name: {} port: {}", name, port);
        }
    }

    public void stop() {
        ServerLauncher.ServerState state = launcher.stop();
        if (!AbstractLauncher.Status.STOPPED.equals(state.getStatus())) {
            logger.warn("Could not stop server, name: {} port: {}", name, port);
        }
    }
}
