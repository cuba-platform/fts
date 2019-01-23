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
