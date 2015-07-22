/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.impl.*;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(ConfigLoader.NAME)
public class ConfigLoader {

    public static final String NAME = "fts_ConfigLoader";

    private Log log = LogFactory.getLog(ConfigLoader.class);

    protected static final String DEFAULT_CONFIG = "/cuba-fts.xml";

    protected static String[] systemProps = new String[]{
            "id", "createTs", "createdBy", "version", "updateTs", "updatedBy", "deleteTs", "deletedBy"
    };
    protected String confDir;

    static {
        Arrays.sort(systemProps);
    }

    @Inject
    private Metadata metadata;

    @Inject
    public void setConfiguration(Configuration configuration) {
        confDir = configuration.getConfig(GlobalConfig.class).getConfDir();
    }

    public Map<String, EntityDescr> loadConfiguration() {
        HashMap<String, EntityDescr> map = new HashMap<>();

        String configName = AppContext.getProperty("cuba.ftsConfig");
        if (StringUtils.isBlank(configName))
            configName = DEFAULT_CONFIG;

        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        StrTokenizer tokenizer = new StrTokenizer(configName);
        for (String location : tokenizer.getTokenArray()) {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                InputStream stream = null;
                try {
                    stream = resource.getInputStream();
                    loadFromStream(stream, map);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            } else {
                log.warn("Resource " + location + " not found, ignore it");
            }
        }

        return map;
    }

    protected void loadFromStream(InputStream stream, Map<String, EntityDescr> map) {
        Document document = Dom4j.readDocument(stream);
        for (Element element : Dom4j.elements(document.getRootElement(), "include")) {
            String fileName = element.attributeValue("file");
            if (!StringUtils.isBlank(fileName)) {
                InputStream incStream = getClass().getResourceAsStream(fileName);
                loadFromStream(incStream, map);
            }
        }

        Element rootElem = document.getRootElement();
        Element entitiesElem = rootElem.element("entities");
        for (Element entityElem : Dom4j.elements(entitiesElem, "entity")) {
            String className = entityElem.attributeValue("class");
            MetaClass metaClass = metadata.getClassNN(ReflectionHelper.getClass(className));
            if (!metadata.getTools().isPersistent(metaClass)) {
                log.warn("Entity " + metaClass.getName() + " is not persistent, ignore it");
                continue;
            }
            metaClass = metadata.getExtendedEntities().getEffectiveMetaClass(metaClass);
            EntityDescr entityDescr = createEntityDescr(entityElem, metaClass);
            map.put(className, entityDescr);
        }
    }

    protected EntityDescr getNewEntityDescr(MetaClass metaClass, String searchableIfScript, String searchablesScript,
                                            boolean show) {
        return new EntityDescr(metaClass, searchableIfScript, searchablesScript, show);
    }

    protected EntityDescr createEntityDescr(Element entityElem, MetaClass metaClass) {
        Element searchableIfScriptElem = entityElem.element("searchableIf");
        String searchableIfScript = searchableIfScriptElem != null ? searchableIfScriptElem.getText() : null;

        Element searchablesScriptElem = entityElem.element("searchables");
        String searchablesScript = searchablesScriptElem != null ? searchablesScriptElem.getText() : null;

        String showStr = entityElem.attributeValue("show");
        boolean show = showStr == null || Boolean.valueOf(showStr);

        EntityDescr entityDescr = getNewEntityDescr(metaClass, searchableIfScript, searchablesScript, show);
        setIncludedFields(entityElem, metaClass, entityDescr);
        setExcludedFields(entityElem, metaClass, entityDescr);
        return entityDescr;
    }

    protected void setIncludedFields(Element entityElem, MetaClass metaClass, EntityDescr entityDescr) {
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
    }

    protected void setExcludedFields(Element entityElem, MetaClass metaClass, EntityDescr entityDescr) {
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
    }

    protected void addPropertyToDescription(EntityDescr descr, String property) {
        descr.addProperty(property);
    }

    protected void removePropertyFromDescription(EntityDescr descr, String property) {
        descr.removeProperty(property);
    }

    protected void includeByName(EntityDescr descr, MetaClass metaClass, String name) {
        if (metaClass.getPropertyPath(name) != null)
            addPropertyToDescription(descr, name);
    }

    protected void includeByRe(EntityDescr descr, MetaClass metaClass, String re) {
        Pattern pattern = Pattern.compile(re);
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (isSearchableProperty(metaProperty)) {
                Matcher matcher = pattern.matcher(metaProperty.getName());
                if (matcher.matches())
                    addPropertyToDescription(descr, metaProperty.getName());
            }
        }
    }

    protected void excludeByName(EntityDescr descr, MetaClass metaClass, String name) {
        removePropertyFromDescription(descr, name);
    }

    protected void excludeByRe(EntityDescr descr, MetaClass metaClass, String re) {
        Pattern pattern = Pattern.compile(re);
        for (String property : descr.getPropertyNames()) {
            Matcher matcher = pattern.matcher(property);
            if (matcher.matches())
                removePropertyFromDescription(descr, property);
        }
    }

    protected boolean isSearchableProperty(MetaProperty metaProperty) {
        if (Arrays.binarySearch(systemProps, metaProperty.getName()) >= 0)
            return false;

        if (metaProperty.getRange().isDatatype()) {
            Datatype dt = metaProperty.getRange().asDatatype();
            return (Datatypes.get(StringDatatype.NAME).equals(dt)
                    || Datatypes.get(DateDatatype.NAME).equals(dt)
                    || Datatypes.get(BigDecimalDatatype.NAME).equals(dt)
                    || Datatypes.get(IntegerDatatype.NAME).equals(dt)
                    || Datatypes.get(DoubleDatatype.NAME).equals(dt));

        } else if (metaProperty.getRange().isEnum() || metaProperty.getRange().isClass()) {
            return true;
        }

        return false;
    }
}
