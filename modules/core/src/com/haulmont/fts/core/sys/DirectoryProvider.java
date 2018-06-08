/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.fts.global.FtsConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class is used for getting an instance of the Lucene index {@link Directory}
 */
@Component("fts_DirectoryProvider")
public class DirectoryProvider {

    protected volatile Directory directory;

    @Inject
    protected FtsConfig ftsConfig;

    @Inject
    protected GlobalConfig globalConfig;

    public Directory getDirectory() {
        if (directory == null) {
            synchronized (this) {
                if (directory == null) {
                    String dir = ftsConfig.getIndexDir();
                    if (StringUtils.isBlank(dir)) {
                        dir = globalConfig.getDataDir() + "/ftsindex";
                    }
                    Path file = Paths.get(dir);
                    if (!Files.exists(file)) {
                        try {
                            Files.createDirectory(file);
                        } catch (IOException e) {
                            throw new RuntimeException("Directory " + dir + " doesn't exist and can not be created");
                        }
                    }
                    try {
                        directory = FSDirectory.open(file);
                        if (Files.exists(file.resolve("write.lock"))) {
                            directory.deleteFile("write.lock");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error on lucene index directory initialization", e);
                    }
                }
            }
        }
        return directory;
    }
}
