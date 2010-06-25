/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.06.2010 19:19:00
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigLoader {

    private static Log log = LogFactory.getLog(ConfigLoader.class);
    
    private static final String DEFAULT_CONFIG = "cuba/fts-config.xml";

    private static String[] systemProps = new String[] {
            "id", "createTs", "createdBy", "version", "updateTs", "updatedBy", "deleteTs", "deletedBy"
    };

    static {
        Arrays.sort(systemProps);
    }

    public Map<String, Set<String>> loadConfiguration() {
        HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();

        String configName = AppContext.getProperty("cuba.fts.config");
        if (StringUtils.isBlank(configName))
            configName = DEFAULT_CONFIG;

        File file = new File(ConfigProvider.getConfig(GlobalConfig.class).getConfDir() + "/" + configName);
        if (!file.exists()) {
            log.error("FTS config file not found: " + file.getAbsolutePath());
            return map;
        }

        Document document;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            document = Dom4j.readDocument(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //
                }
        }
        Element rootElem = document.getRootElement();
        Element entitiesElem = rootElem.element("entities");
        for (Element entityElem : Dom4j.elements(entitiesElem, "entity")) {
            String className = entityElem.attributeValue("class");
            MetaClass metaClass = MetadataProvider.getSession().getClass(ReflectionHelper.getClass(className));

            Set<String> properties = new HashSet<String>();

            boolean explicitInclude = false;
            for (Element element : Dom4j.elements(entityElem, "include")) {
                String re = element.attributeValue("re");
                if (!StringUtils.isBlank(re))
                    includeByRe(properties, metaClass, re);
                else {
                    String name = element.attributeValue("name");
                    if (!StringUtils.isBlank(name))
                        includeByName(properties, metaClass, name);
                }
                explicitInclude = true;
            }
            if (!explicitInclude) {
                includeByRe(properties, metaClass, ".*");
            }

            for (Element element : Dom4j.elements(entityElem, "exclude")) {
                String re = element.attributeValue("re");
                if (!StringUtils.isBlank(re))
                    excludeByRe(properties, metaClass, re);
                else {
                    String name = element.attributeValue("name");
                    if (!StringUtils.isBlank(name))
                        excludeByName(properties, metaClass, name);
                }
            }

            map.put(className, properties);
        }

        return map;
    }

    private void includeByName(Set<String> properties, MetaClass metaClass, String name) {
        if (metaClass.getProperty(name) != null)
            properties.add(name);
    }

    private void includeByRe(Set<String> properties, MetaClass metaClass, String re) {
        Pattern pattern = Pattern.compile(re);
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (!isSystemProperty(metaProperty)) {
                Matcher matcher = pattern.matcher(metaProperty.getName());
                if (matcher.matches())
                    properties.add(metaProperty.getName());
            }
        }
    }

    private void excludeByName(Set<String> properties, MetaClass metaClass, String name) {
        if (metaClass.getProperty(name) != null)
            properties.remove(name);
    }

    private void excludeByRe(Set<String> properties, MetaClass metaClass, String re) {
        Pattern pattern = Pattern.compile(re);
        for (String property : new ArrayList<String>(properties)) {
            Matcher matcher = pattern.matcher(property);
            if (matcher.matches())
                properties.remove(property);
        }
    }

    private boolean isSystemProperty(MetaProperty metaProperty) {
        return Arrays.binarySearch(systemProps, metaProperty.getName()) >= 0;
    }
}
