/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(ConfigLoader.NAME)
public class ConfigLoader {

    public static final String NAME = "fts_ConfigLoader";

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    protected static final String DEFAULT_CONFIG = "/com/haulmont/fts/fts.xml";

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
        HashMap<String, EntityDescr> map = new LinkedHashMap<>();

        String configName = AppContext.getProperty("cuba.ftsConfig");
        if (StringUtils.isBlank(configName))
            configName = DEFAULT_CONFIG;

        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        StringTokenizer tokenizer = new StringTokenizer(configName);
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
        for (Element element : document.getRootElement().elements("include")) {
            String fileName = element.attributeValue("file");
            if (!StringUtils.isBlank(fileName)) {
                InputStream incStream = getClass().getResourceAsStream(fileName);
                loadFromStream(incStream, map);
            }
        }

        Element rootElem = document.getRootElement();
        Element entitiesElem = rootElem.element("entities");
        for (Element entityElem : entitiesElem.elements("entity")) {
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
        for (Element element : entityElem.elements("include")) {
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
        for (Element element : entityElem.elements("exclude")) {
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
        if (metaClass.getPropertyPath(name) != null) {
            addPropertyToDescription(descr, name);
        }
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
            Class type = metaProperty.getRange().asDatatype().getJavaClass();
            return (type.equals(String.class)
                    || type.equals(java.sql.Date.class)
                    || type.equals(BigDecimal.class)
                    || type.equals(Integer.class)
                    || type.equals(Long.class)
                    || type.equals(Double.class));

        } else if (metaProperty.getRange().isEnum() || metaProperty.getRange().isClass()) {
            return true;
        }

        return false;
    }
}
