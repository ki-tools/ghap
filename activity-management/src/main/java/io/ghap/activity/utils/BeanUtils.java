package io.ghap.activity.utils;

import org.apache.commons.beanutils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 */
public class BeanUtils {

    private static final Logger log = LoggerFactory.getLogger(BeanUtils.class);

    public static Object copyProperties(Object source, Object target) {
        return copyProperties(source, target, null);
    }

    public static Object copyProperties(Object source, Object target, List<String> excludes) {
        if (source == null || target == null) {
            return target;
        }
        try {
            __copyProperties(source, target, excludes);
        } catch (IllegalAccessException e) {
            log.error("cannot copy properties", e);
        } catch (InvocationTargetException e) {
            log.error("cannot copy properties", e);
        }
        return target;
    }

    private static void __copyProperties(Object orig, Object dest, List<String> excludes)
            throws IllegalAccessException, InvocationTargetException {
        PropertyUtilsBean propertyUtils = BeanUtilsBean.getInstance().getPropertyUtils();
        if (excludes == null) {
            excludes = Collections.EMPTY_LIST;
        }
        // Validate existence of the specified beans
        if (dest == null) {
            throw new IllegalArgumentException
                    ("No destination bean specified");
        }
        if (orig == null) {
            throw new IllegalArgumentException("No origin bean specified");
        }
        if (log.isDebugEnabled()) {
            log.debug("BeanUtils.copyProperties(" + dest + ", " +
                    orig + ")");
        }

        // Copy the properties, converting as necessary
        if (orig instanceof DynaBean) {
            DynaProperty[] origDescriptors =
                    ((DynaBean) orig).getDynaClass().getDynaProperties();
            for (int i = 0; i < origDescriptors.length; i++) {
                String name = origDescriptors[i].getName();
                // Need to check isReadable() for WrapDynaBean
                // (see Jira issue# BEANUTILS-61)
                if (propertyUtils.isReadable(orig, name) &&
                        propertyUtils.isWriteable(dest, name)) {
                    Object value = ((DynaBean) orig).get(name);
                    BeanUtilsBean.getInstance().copyProperty(dest, name, value);
                }
            }
        } else if (orig instanceof Map) {
            @SuppressWarnings("unchecked")
            // Map properties are always of type <String, Object>
                    Map<String, Object> propMap = (Map<String, Object>) orig;
            for (Map.Entry<String, Object> entry : propMap.entrySet()) {
                String name = entry.getKey();
                if (propertyUtils.isWriteable(dest, name)) {
                    BeanUtilsBean.getInstance().copyProperty(dest, name, entry.getValue());
                }
            }
        } else /* if (orig is a standard JavaBean) */ {
            PropertyDescriptor[] origDescriptors =
                    propertyUtils.getPropertyDescriptors(orig);
            for (int i = 0; i < origDescriptors.length; i++) {
                String name = origDescriptors[i].getName();
                if ("class".equals(name)) {
                    continue; // No point in trying to set an object's class
                }
                if (excludes.contains(name)) {
                    continue;
                }
                if (propertyUtils.isReadable(orig, name) &&
                        propertyUtils.isWriteable(dest, name)) {
                    try {
                        Object value =
                                propertyUtils.getSimpleProperty(orig, name);
                        BeanUtilsBean.getInstance().copyProperty(dest, name, value);
                    } catch (NoSuchMethodException e) {
                        // Should not happen
                    }
                }
            }
        }

    }
}
