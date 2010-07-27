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
import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.impl.*;
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
    protected String confDir;

    static {
        Arrays.sort(systemProps);
    }

    public ConfigLoader() {
        confDir = ConfigProvider.getConfig(GlobalConfig.class).getConfDir();
    }

    public Map<String, EntityDescr> loadConfiguration() {
        HashMap<String, EntityDescr> map = new HashMap<String, EntityDescr>();

        String configName = AppContext.getProperty("cuba.fts.config");
        if (StringUtils.isBlank(configName))
            configName = DEFAULT_CONFIG;

        File file = new File(confDir + "/" + configName);
        if (!file.exists()) {
            log.error("FTS config file not found: " + file.getAbsolutePath());
            return map;
        }

        loadFromFile(file, map);

        return map;
    }

    private void loadFromFile(File file, Map<String, EntityDescr> map) {
        Document document = Dom4j.readDocument(file);
        for (Element element : Dom4j.elements(document.getRootElement(), "include")) {
            String fileName = element.attributeValue("file");
            if (!StringUtils.isBlank(fileName)) {
                File incFile = new File(confDir + "/" + fileName);
                loadFromFile(incFile, map);
            }
        }

        Element rootElem = document.getRootElement();
        Element entitiesElem = rootElem.element("entities");
        for (Element entityElem : Dom4j.elements(entitiesElem, "entity")) {
            String className = entityElem.attributeValue("class");
            MetaClass metaClass = MetadataProvider.getSession().getClass(ReflectionHelper.getClass(className));

            Element scriptElem = entityElem.element("searchable");
            String script = scriptElem != null ? scriptElem.getText() : null;

            EntityDescr entityDescr = new EntityDescr(metaClass, entityElem.attributeValue("view"), script);

            for (Element element : Dom4j.elements(entityElem, "include")) {
                String re = element.attributeValue("re");
                if (!StringUtils.isBlank(re))
                    includeByRe(entityDescr, metaClass, re);
                else {
                    String name = element.attributeValue("name");
                    if (!StringUtils.isBlank(name))
                        includeByName(entityDescr, metaClass, name);
                }
            }

            for (Element element : Dom4j.elements(entityElem, "exclude")) {
                String re = element.attributeValue("re");
                if (!StringUtils.isBlank(re))
                    excludeByRe(entityDescr, metaClass, re);
                else {
                    String name = element.attributeValue("name");
                    if (!StringUtils.isBlank(name))
                        excludeByName(entityDescr, metaClass, name);
                }
            }

            map.put(className, entityDescr);
        }
    }

    private void includeByName(EntityDescr descr, MetaClass metaClass, String name) {
        if (metaClass.getPropertyEx(name) != null)
            descr.addProperty(name);
    }

    private void includeByRe(EntityDescr descr, MetaClass metaClass, String re) {
        Pattern pattern = Pattern.compile(re);
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (isSearchableProperty(metaProperty)) {
                Matcher matcher = pattern.matcher(metaProperty.getName());
                if (matcher.matches())
                    descr.addProperty(metaProperty.getName());
            }
        }
    }

    private void excludeByName(EntityDescr descr, MetaClass metaClass, String name) {
        descr.removeProperty(name);
    }

    private void excludeByRe(EntityDescr descr, MetaClass metaClass, String re) {
        Pattern pattern = Pattern.compile(re);
        for (String property : descr.getPropertyNames()) {
            Matcher matcher = pattern.matcher(property);
            if (matcher.matches())
                descr.removeProperty(property);
        }
    }

    private boolean isSearchableProperty(MetaProperty metaProperty) {
        if (Arrays.binarySearch(systemProps, metaProperty.getName()) >= 0)
            return false;

        if (metaProperty.getRange().isDatatype()) {
            Datatype dt = metaProperty.getRange().asDatatype();
            return (Datatypes.getInstance().get(StringDatatype.NAME).equals(dt)
                    || Datatypes.getInstance().get(DateDatatype.NAME).equals(dt)
                    || Datatypes.getInstance().get(BigDecimalDatatype.NAME).equals(dt)
                    || Datatypes.getInstance().get(IntegerDatatype.NAME).equals(dt)
                    || Datatypes.getInstance().get(DoubleDatatype.NAME).equals(dt));

        } else if (metaProperty.getRange().isEnum() || metaProperty.getRange().isClass()) {
            return true;
        }

        return false;
    }
}
