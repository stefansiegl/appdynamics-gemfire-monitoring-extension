package de.novatec.appdynamics.extensions.gemfire.connection;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic proxy for the connection to the JMX server that allows for transparent retry operations in case one request fails.
 *
 * Currently not in use.
 *
 * @author Stefan Siegl (sieglst@googlemail.com)
 * @author Stefan Siegl - APM competence group  NovaTec Consulting (stefan.siegl@novatec-gmbh.de)
 */
public class RetryMBeanConnection implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RetryMBeanConnection.class);

    private final static int MAXIMUM_RETRIES = 5;

    private JMXConnector connector;
    private MBeanServerConnection connection;
    private JMXServiceURL serviceUrl;
    private String username;
    private String password;
    private int errorCount = 0;

    private RetryMBeanConnection(JMXServiceURL serviceUrl, String username, String password) {
        this.serviceUrl = serviceUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Provides a proxied MBeanServerConnection that automatically and transparently to the client performs reconnects
     * in case of temporary connection problems.
     *
     * @param serviceUrl the connection URL to the JMX server
     * @param username   the username
     * @param password   the passwort
     * @return a proxied MBeanServerConnection that automatically and transparently to the client performs reconnects
     * in case of temporary connection problems.
     * @throws IOException thrown if after a number of retries connection problems still exist.
     */
    public static MBeanServerConnection getRetryConnectionFor(@Nonnull JMXServiceURL serviceUrl, String username, String password) throws IOException {
        RetryMBeanConnection retryMBeanConnection = new RetryMBeanConnection(serviceUrl, username, password);
        return (MBeanServerConnection) Proxy.newProxyInstance(MBeanServerConnection.class.getClassLoader(), new Class[]{MBeanServerConnection.class}, retryMBeanConnection);
    }

    private void resetConnection() {
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

    private MBeanServerConnection getConnection() throws IOException {
        if (connection == null) {
           // newConnection();
        }
        return connection;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            result = method.invoke(getConnection(), args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IOException e) {
            errorCount++;
            if (errorCount <= MAXIMUM_RETRIES) {
                logger.debug("Got IOException calling to JMX server for Method " + method + " retrying.");
            //    newConnection();
                invoke(proxy, method, args);
            } else {
                logger.error("Multiple tries to connect to the JMX server failed. ", e);
                errorCount = 0;
                throw e;
            }
        } catch (Exception e) {
            throw e;
        }
        return result;
    }
}
