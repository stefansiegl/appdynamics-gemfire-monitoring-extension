package de.novatec.appdynamics.extensions.gemfire.util;

/**
 * Exception that is thrown in case a wrong data type is provided.
 */
public class InvalidAppDMetricTypeException extends Exception {
    public InvalidAppDMetricTypeException(String message) {
        super(message);
    }
}
