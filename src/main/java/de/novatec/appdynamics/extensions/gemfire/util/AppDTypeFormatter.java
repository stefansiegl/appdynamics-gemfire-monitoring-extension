package de.novatec.appdynamics.extensions.gemfire.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some util methods.
 *
 * @author Stefan Siegl (sieglst@googlemail.com)
 * @author Stefan Siegl - APM competence group NovaTec Consulting (stefan.siegl@novatec-gmbh.de)
 */
public class AppDTypeFormatter {
    private static final Logger logger = LoggerFactory.getLogger(AppDTypeFormatter.class);

    private static final Pattern PATTERN_REGION_SERVER_DETAIL = Pattern.compile("GemFire:service=Region,name=(.*),type=Member,member=(.*)");
    private static final Pattern PATTERN_SYSTEM = Pattern.compile("GemFire:service=System,type=Distributed");
    private static final Pattern PATTERN_SERVER = Pattern.compile("GemFire:type=Member,member=(.*)");
    private static final Pattern PATTERN_REGION_DETAILS = Pattern.compile("GemFire:service=Region,name=(.*),type=Distributed");

    /**
     * Converts the given numerical value to BigDecimal (necessary for AppDynamics) and applies the scaling factor. Do
     * note that AppDynamics (4.3) does not support sting-based values.
     *
     * @param o     the numerical object.
     * @param scale the scaling factor.
     * @return BigDecimal of the value.
     */
    public BigDecimal convert(Object o, int scale) throws InvalidAppDMetricTypeException {
        if (o instanceof Long) {
            return BigDecimal.valueOf(((Long) o) * scale);
        } else if (o instanceof Integer) {
            return BigDecimal.valueOf(new Long(((Integer) o) * scale));
        } else if (o instanceof Float) {
            Float f = (Float) o;
            float val = f.floatValue() * scale;
            return BigDecimal.valueOf(new Float(val).longValue());
        } else if (o instanceof Double) {
            Double d = (Double) o;
            double val = d.doubleValue() * scale;
            return BigDecimal.valueOf(new Double(val).longValue());
        } else if (o instanceof String) {
            throw new InvalidAppDMetricTypeException("String values cannot be used as metrics for AppDynamics. Current string value is " + o);
        }

        logger.warn("Unknown numerical class pattern, please provide a mapping to BigDecimal for class: " + o.getClass().getName() + " with value " + o + ". Trying a direct cast");
        return (BigDecimal) o;
    }

    /**
     * Formats the given JMX objectname to match the requirements of AppDynamics.
     *
     * @param objectName the object name
     * @return formatted string.
     */
    public String formatObjectName(String objectName) {
        // GemFire:service=System,type=Distributed
        if (PATTERN_SYSTEM.matcher(objectName).matches()) {
            return "System";
        }

        // GemFire:service=Region,name=/regionA,type=Member,member=server1
        Matcher regionServerDetails = PATTERN_REGION_SERVER_DETAIL.matcher(objectName);
        if (regionServerDetails.matches()) {
            return "RegionServerDetails|" + regionServerDetails.group(1) + "|" + regionServerDetails.group(2);
        }

        // GemFire:service=Region,name=/regionA,type=Distributed
        Matcher regionDetails = PATTERN_REGION_DETAILS.matcher(objectName);
        if (regionDetails.matches()) {
            return "Regions|" + regionDetails.group(1);
        }

        // GemFire:type=Member,member=locator1
        Matcher serverDetails = PATTERN_SERVER.matcher(objectName);
        if (serverDetails.matches()) {
            return "ServerDetails|" + serverDetails.group(1);
        }

        // else - new pattern that I did not think on ;)
        logger.warn("Unknown objectname pattern, please provide a mapping to an appdynamics name. Using \"unknown\" for name {}. ", objectName);
        return "unknown";
    }
}
