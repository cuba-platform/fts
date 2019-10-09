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


import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.fts.FtsTestContainer
import com.haulmont.fts.app.FtsService
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class HitPropertyCaptionTest extends Specification {

    @Shared
    @ClassRule
    public FtsTestContainer cont = FtsTestContainer.Common.INSTANCE

    private FtsService ftsService

    void setup() {
        ftsService = AppBeans.get(FtsService)
    }

    void "get hit property caption for simple entity attribute"() {
        when:
        def caption = ftsService.getHitPropertyCaption('ftstest_Book', 'title')

        then:
        caption == "Title"
    }

    void "get hit property caption for FileDescriptor reference"() {
        when:
        def caption = ftsService.getHitPropertyCaption('ftstest_Book', 'bookFile')

        then:
        caption == "Book file"
    }

    void "get hit property caption for FileDescriptor reference (name)"() {
        when:
        def caption = ftsService.getHitPropertyCaption('ftstest_Book', 'bookFile.name')

        then:
        caption == "Book file.Name"
    }

    void "get hit property caption for FileDescriptor reference (fileContent)"() {
        when:
        def caption = ftsService.getHitPropertyCaption('ftstest_Book', 'bookFile.fileContent.Some.file.name.txt')

        then:
        caption == "Book file.Contents [Some.file.name.txt]"
    }

    void "get hit property caption for FileDescriptor of collection attribute"() {
        when:
        def caption = ftsService.getHitPropertyCaption('ftstest_Book', 'bookReviews.reviewFile.fileContent.Some.file.name.txt')

        then:
        caption == "Book reviews.Review file.Contents [Some.file.name.txt]"
    }
}
