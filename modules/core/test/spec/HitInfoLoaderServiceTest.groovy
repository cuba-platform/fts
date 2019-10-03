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

package spec

import com.haulmont.bali.db.QueryRunner
import com.haulmont.cuba.core.Persistence
import com.haulmont.cuba.core.app.FileStorageService
import com.haulmont.cuba.core.entity.FileDescriptor
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.DataManager
import com.haulmont.cuba.core.global.Metadata
import com.haulmont.cuba.core.global.TimeSource
import com.haulmont.fts.FtsTestContainer
import com.haulmont.fts.app.FtsService
import com.haulmont.fts.app.HitInfoLoaderService
import com.haulmont.fts.core.app.FtsManagerAPI
import com.haulmont.fts.global.HitInfo
import com.haulmont.fts.testmodel.searchresult.Book
import org.apache.commons.io.IOUtils
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class HitInfoLoaderServiceTest extends Specification {

    @Shared
    @ClassRule
    public FtsTestContainer cont = FtsTestContainer.Common.INSTANCE

    private Persistence persistence
    private Metadata metadata
    private FtsManagerAPI ftsManager
    private FtsService ftsService
    private DataManager dataManager
    private TimeSource timeSource
    private FileStorageService fileStorageService
    private HitInfoLoaderService hitInfoLoaderService

    private UUID BOOK_ID = UUID.randomUUID()

    void setup() {
        persistence = cont.persistence()
        metadata = cont.metadata()
        ftsManager = AppBeans.get(FtsManagerAPI)
        ftsService = AppBeans.get(FtsService)
        dataManager = AppBeans.get(DataManager)
        timeSource = AppBeans.get(TimeSource)
        fileStorageService = AppBeans.get(FileStorageService)
        hitInfoLoaderService = AppBeans.get(HitInfoLoaderService)

        ftsManager.setEnabled(true)
    }

    void cleanup() {
        clearData()
        ftsManager.setEnabled(false)
    }

    void "test loadHitInfos()"() {

        given:

        prepareTestData()
        reindexData()
        String searchTerm = 'amazing'

        when:

        List<HitInfo> hitInfos = hitInfoLoaderService.loadHitInfos('ftstest_Book', BOOK_ID, searchTerm)

        then:

        hitInfos.size() == 3

        HitInfo title_hit = hitInfos.find {it.fieldName == 'title'}
        title_hit.highlightedText == 'The <b>amazing</b> book'

        HitInfo bookFile_name_it = hitInfos.find {it.fieldName == 'bookFile.name'}
        bookFile_name_it.highlightedText == 'the.<b>amazing</b>.book.txt'

        HitInfo bookFile_fileContent_hit = hitInfos.find {it.fieldName == 'bookFile.fileContent.the.amazing.book.txt'}
        bookFile_fileContent_hit.highlightedText == '... content of the <b>amazing</b> book file...'
    }

    protected void prepareTestData() {
        Book book = metadata.create(Book)
        book.id = BOOK_ID
        book.title = "The amazing book"

        FileDescriptor fd = metadata.create(FileDescriptor)
        fd.name = "the.amazing.book.txt"
        fd.extension = "txt"
        fd.createDate = timeSource.currentTimestamp()
        fileStorageService.saveFile(fd, loadFixtureData("the.amazing.book.txt"))

        book.bookFile = fd

        dataManager.commit(book, fd)
    }

    void reindexData() {
        ftsManager.reindexAll()
        Integer count = null
        while (count == null || count > 0) {
            count = ftsManager.processQueue()
        }
    }

    protected void clearData() {
        new QueryRunner(persistence.getDataSource()).update("delete from FTS_TEST_BOOK")
        new QueryRunner(persistence.getDataSource()).update("delete from SYS_FILE")
        new QueryRunner(persistence.getDataSource()).update("delete from SYS_FTS_QUEUE")
    }

    protected byte[] loadFixtureData(String fileName) {
        return IOUtils.toByteArray((InputStream) getClass().getResourceAsStream("fixture/" + fileName))
    }
}
