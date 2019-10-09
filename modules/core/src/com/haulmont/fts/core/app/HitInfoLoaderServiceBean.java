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

package com.haulmont.fts.core.app;

import com.google.common.base.Strings;
import com.haulmont.fts.app.HitInfoLoaderService;
import com.haulmont.fts.core.sys.AllDocsCollector;
import com.haulmont.fts.core.sys.HitInfoTextsBuilder;
import com.haulmont.fts.core.sys.IndexSearcherProvider;
import com.haulmont.fts.global.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static com.haulmont.fts.core.sys.LuceneConstants.*;

@Service(HitInfoLoaderService.NAME)
public class HitInfoLoaderServiceBean implements HitInfoLoaderService {

    @Inject
    protected IndexSearcherProvider indexSearcherProvider;

    @Inject
    protected HitInfoTextsBuilder hitInfoTextsBuilder;

    @Inject
    protected Logger log;

    @Override
    public void populateHitInfos(SearchResult searchResult) {
        IndexSearcher searcher = null;
        try {
            searcher = indexSearcherProvider.acquireIndexSearcher();
            String searchTerm = searchResult.getSearchTerm();
            for (SearchResultEntry searchResultEntry : searchResult.getAllEntries()) {
                List<HitInfo> hitInfoList = new ArrayList<>();
                EntityInfo entityInfo = searchResultEntry.getEntityInfo();
                Query query = createQueryForEntityInfoSearch(entityInfo);
                TopDocs topDocs = searcher.search(query, 1);
                if (topDocs.scoreDocs.length == 0) {
                    log.warn("No result found for {}", entityInfo);
                    continue;
                }
                Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
                if (searchResultEntry.isDirectResult()) {
                    String fldAllText = doc.getField(FLD_ALL).stringValue();
                    Map<String, String> hits = hitInfoTextsBuilder.buildHighlightedHitTexts(fldAllText, searchTerm);
                    for (Map.Entry<String, String> entry : hits.entrySet()) {
                        String fieldName = entry.getKey();
                        String highlightedText = entry.getValue();
                        HitInfo hitInfo = new HitInfo(fieldName, highlightedText);
                        hitInfoList.add(hitInfo);
                    }
                }

                //Builds hit infos from linked entities
                //For each entity info defined in the "links" field we load a lucene document and evaluate hit texts from the "all" field of the
                //loaded linked document
                List<EntityInfo> linkedEntityInfos = searchResultEntry.getLinkedEntityInfos();
                if (!linkedEntityInfos.isEmpty()) {
                    String fldLinks = doc.getField(FLD_LINKS).stringValue();
                    for (EntityInfo linkedEntityInfo : linkedEntityInfos) {
                        Query linkedEntityQuery = createQueryForEntityInfoSearch(linkedEntityInfo);
                        TopDocs linkedTopDocs = searcher.search(linkedEntityQuery, 1);
                        if (linkedTopDocs.scoreDocs.length == 0) {
                            log.warn("No result found for linked entity {}", linkedEntityInfo);
                            continue;
                        }
                        Document linkedDoc = searcher.doc(linkedTopDocs.scoreDocs[0].doc);
                        String linkedFldAll = linkedDoc.getField(FLD_ALL).stringValue();
                        Map<String, String> linkedHits = hitInfoTextsBuilder.buildHighlightedHitTexts(linkedFldAll, searchTerm);

                        Set<String> linkedEntityFieldNames = getLinkedEntityFieldNames(fldLinks, linkedEntityInfo);
                        for (String linkedEntityFieldName : linkedEntityFieldNames) {
                            for (Map.Entry<String, String> entry : linkedHits.entrySet()) {
                                String fieldName = !Strings.isNullOrEmpty(linkedEntityFieldName) ?
                                        linkedEntityFieldName + "." + entry.getKey() :
                                        entry.getKey();
                                String highlightedText = entry.getValue();
                                HitInfo hitInfo = new HitInfo(fieldName, highlightedText);
                                hitInfoList.add(hitInfo);
                            }
                        }
                    }
                }
                searchResultEntry.setHitInfos(hitInfoList);
            }
        } catch (IOException e) {
            throw new RuntimeException("Search error", e);
        } finally {
            if (searcher != null)
                indexSearcherProvider.releaseIndexSearcher(searcher);
        }
    }

    @Override
    public List<HitInfo> loadHitInfos(String entityName, Object entityId, String searchTerm) {
        List<HitInfo> hitInfoList = new ArrayList<>();
        IndexSearcher searcher = null;
        EntityInfo entityInfo = new EntityInfo(entityName, entityId);
        try {
            searcher = indexSearcherProvider.acquireIndexSearcher();
            Query query = createQueryForEntityInfoSearch(entityInfo);
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.scoreDocs.length == 0) {
                log.warn("No result found for {}", entityInfo);
                return hitInfoList;
            }

            Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
            String fldAllText = doc.getField(FLD_ALL).stringValue();
            Map<String, String> hits = hitInfoTextsBuilder.buildHighlightedHitTexts(fldAllText, searchTerm);
            for (Map.Entry<String, String> entry : hits.entrySet()) {
                String fieldName = entry.getKey();
                String highlightedText = entry.getValue();
                HitInfo hitInfo = new HitInfo(fieldName, highlightedText);
                hitInfoList.add(hitInfo);
            }

            String fldLinks = doc.getField(FLD_LINKS).stringValue();
            if (!Strings.isNullOrEmpty(fldLinks)) {
                List<HitInfo> linkedHitInfos = buildHitInfosForLinkedEntities(fldLinks, searchTerm, searcher);
                hitInfoList.addAll(linkedHitInfos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Search error", e);
        } finally {
            if (searcher != null)
                indexSearcherProvider.releaseIndexSearcher(searcher);
        }

        return hitInfoList;
    }

    protected Query createQueryForEntityInfoSearch(EntityInfo entityInfo) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        TermQuery idQuery = new TermQuery(new Term(FLD_ID, entityInfo.getId().toString()));
        TermQuery entityNameQuery = new TermQuery(new Term(FLD_ENTITY, entityInfo.getEntityName()));
        builder.add(idQuery, BooleanClause.Occur.MUST);
        builder.add(entityNameQuery, BooleanClause.Occur.MUST);
        return builder.build();
    }

    protected List<HitInfo> buildHitInfosForLinkedEntities(String linksField, String searchTerm, IndexSearcher searcher) throws IOException {
        List<HitInfo> hitInfoList = new ArrayList<>();
        String[] parts = linksField.split("\\s+");
        String linkedEntityFieldName = null;
        String linkedEntityInfoStr = null;
        for (String part : parts) {
            if (part.startsWith(FTS.FIELD_START)) {
                linkedEntityFieldName = part.substring(2).replace(FTS.FIELD_SEP, ".");
                continue;
            } else {
                linkedEntityInfoStr = part;
                String linkedEntityName = linkedEntityInfoStr.substring(0, linkedEntityInfoStr.indexOf("-"));
                String linkedEntityId = linkedEntityInfoStr.substring(linkedEntityInfoStr.indexOf("-") + 1);
                Query linkedEntityQuery = createQueryForEntityInfoSearch(new EntityInfo(linkedEntityName, linkedEntityId));
                AllDocsCollector collector = new AllDocsCollector();
                searcher.search(linkedEntityQuery, collector);
                for (Integer docId : collector.getDocIds()) {
                    Document doc = searcher.doc(docId);
                    String fldAllText = doc.getField(FLD_ALL).stringValue();
                    Map<String, String> hits = hitInfoTextsBuilder.buildHighlightedHitTexts(fldAllText, searchTerm);
                    for (Map.Entry<String, String> entry : hits.entrySet()) {
                        String fieldName = linkedEntityFieldName + "." + entry.getKey();
                        String highlightedText = entry.getValue();
                        HitInfo hitInfo = new HitInfo(fieldName, highlightedText);
                        hitInfoList.add(hitInfo);
                    }
                }
            }
        }
        return hitInfoList;
    }

    /**
     * Parses the document field that stores links to other entities and returns a list of field names that reference to the entity.
     * <p>
     * Examples of the links field:
     * <ul>
     *     <li>If there are several link fields: <i>^^ebook sys$FileDescriptor-bfec4205-11e1-3c7e-9644-7eee3a732fb2 ^^author sample_Author-0872af91-17d4-304e-85b4-b871aa9e41f6</i></li>
     *     <li>If there is a collection with multiple values: <i>^^attachments^file sys$FileDescriptor-3f659ed4-2d7c-895b-fefc-4f8936c2a80d sys$FileDescriptor-4e8f45c3-64d5-6346-c7ba-6697adfe85db</i></li>
     * </ul>
     *
     * @param linksField a content of the field that stores an information about links.
     * @param entityInfo
     * @return a set of fieldNames
     */
    protected Set<String> getLinkedEntityFieldNames(String linksField, EntityInfo entityInfo) {
        Set<String> fieldNames = new HashSet<>();
        String[] parts = linksField.split("\\s+");
        String linkedEntityFieldName = null;
        String linkedEntityInfoStr = null;
        for (String part : parts) {
            if (part.startsWith(FTS.FIELD_START)) {
                linkedEntityFieldName = part.substring(2).replace(FTS.FIELD_SEP, ".");
                continue;
            } else {
                linkedEntityInfoStr = part;
                if (linkedEntityInfoStr.equals(entityInfo.toString())) {
                    fieldNames.add(linkedEntityFieldName);
                }
            }
        }
        return fieldNames;
    }
}
