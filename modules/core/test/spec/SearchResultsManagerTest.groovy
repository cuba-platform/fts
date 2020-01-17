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
import com.haulmont.cuba.core.EntityManager
import com.haulmont.cuba.core.Persistence
import com.haulmont.cuba.core.Transaction
import com.haulmont.cuba.core.entity.Entity
import com.haulmont.cuba.core.global.*
import com.haulmont.cuba.security.auth.AuthenticationManager
import com.haulmont.cuba.security.auth.LoginPasswordCredentials
import com.haulmont.cuba.security.entity.*
import com.haulmont.cuba.security.global.UserSession
import com.haulmont.cuba.testsupport.TestUserSessionSource
import com.haulmont.fts.FtsTestContainer
import com.haulmont.fts.app.FtsService
import com.haulmont.fts.core.app.FtsManagerAPI
import com.haulmont.fts.global.QueryKey
import com.haulmont.fts.global.SearchResult
import com.haulmont.fts.global.SearchResultEntry
import com.haulmont.fts.testmodel.searchresult.MainEntity
import com.haulmont.fts.testmodel.searchresult.RelatedEntity
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Predicate

class SearchResultsManagerTest extends Specification {

    @Shared
    @ClassRule
    public FtsTestContainer cont = FtsTestContainer.Common.INSTANCE

    private Persistence persistence
    private Metadata metadata
    private FtsManagerAPI ftsManager
    private FtsService ftsService
    private DataManager dataManager
    private UserSessionSource userSessionSource
    private AuthenticationManager authenticationManager
    private PasswordEncryption passwordEncryption

    private Group parentGroup
    private Group constraintGroup1, constraintGroup2
    private Constraint constraint1, constraint2
    private User constraintUser1, constraintUser2
    private Role fullAccessRole
    private UserRole userRole1, userRole2

    private static final String PASSWORD = "password"

    void setup() {
        persistence = cont.persistence()
        metadata = cont.metadata()
        ftsManager = AppBeans.get(FtsManagerAPI)
        ftsService = AppBeans.get(FtsService)
        dataManager = AppBeans.get(DataManager)
        passwordEncryption = AppBeans.get(PasswordEncryption)
        userSessionSource = AppBeans.get(UserSessionSource)
        authenticationManager = AppBeans.get(AuthenticationManager)

        ftsManager.setEnabled(true)

        clearData()

        Transaction tx = persistence.createTransaction()
        try {
            EntityManager em = persistence.entityManager

            parentGroup = metadata.create(Group)
            parentGroup.setName("parentGroup")
            em.persist(parentGroup)

            constraintGroup1 = metadata.create(Group)
            constraintGroup1.setName("constraintGroup1")
            em.persist(constraintGroup1)

            constraint1 = metadata.create(Constraint)
            constraint1.setEntityName('ftstest$MainEntity')
            constraint1.setCheckType(ConstraintCheckType.DATABASE)
            constraint1.setOperationType(ConstraintOperationType.READ)
            constraint1.setWhereClause("{E}.rls = '1'")
            constraint1.setGroup(constraintGroup1)
            em.persist(constraint1)

            constraintGroup2 = metadata.create(Group)
            constraintGroup2.setName("constraintGroup2")
            em.persist(constraintGroup2)

            constraint2 = metadata.create(Constraint)
            constraint2.setEntityName('ftstest$RelatedEntity')
            constraint2.setCheckType(ConstraintCheckType.DATABASE)
            constraint2.setOperationType(ConstraintOperationType.READ)
            constraint2.setWhereClause("{E}.rls = '1'")
            constraint2.setGroup(constraintGroup2)
            em.persist(constraint2)

            constraintUser1 = metadata.create(User)
            constraintUser1.setLogin("constraintUser1")
            constraintUser1.setPassword(passwordEncryption.getPasswordHash(constraintUser1.getId(), PASSWORD))
            constraintUser1.setGroup(constraintGroup1)
            em.persist(constraintUser1)

            constraintUser2 = metadata.create(User)
            constraintUser2.setLogin("constraintUser2")
            constraintUser2.setPassword(passwordEncryption.getPasswordHash(constraintUser2.getId(), PASSWORD))
            constraintUser2.setGroup(constraintGroup2)
            em.persist(constraintUser2)

            fullAccessRole = metadata.create(Role)
            fullAccessRole.name = "test-full-access"
            fullAccessRole.defaultEntityReadAccess = Access.ALLOW
            fullAccessRole.defaultEntityAttributeAccess = EntityAttrAccess.MODIFY
            em.persist(this.fullAccessRole)

            userRole1 = metadata.create(UserRole)
            userRole1.role = this.fullAccessRole
            userRole1.user = constraintUser1
            em.persist(userRole1)

            userRole2 = metadata.create(UserRole)
            userRole2.role = this.fullAccessRole
            userRole2.user = constraintUser2
            em.persist(userRole2)

            tx.commit()
        } finally {
            tx.end()
        }
    }

    void cleanup() {
        ftsManager.setEnabled(false)
        clearData()
        cont.deleteRecord("SEC_USER_ROLE", userRole1.getId(), userRole2.getId())
        cont.deleteRecord("SEC_ROLE", fullAccessRole.getId())
        cont.deleteRecord("SEC_USER", constraintUser1.getId(), constraintUser2.getId())
        cont.deleteRecord("SEC_CONSTRAINT", constraint1.getId(), constraint2.getId())
        cont.deleteRecord("SEC_GROUP", parentGroup.getId(), constraintGroup1.getId(), constraintGroup2.getId())
    }

    def "search by term without security constrains"() {
        setup:

        prepareDataForSearchByTerm()
        reindexData()

        when:

        Set<SearchResultEntry> results = searchByIndex('fedor')
        List<Entity> dbResults = searchMainEntityByDb('fedor')

        then:

        assertResults(results, dbResults)
    }

    def "search by term with security constrains"() {
        setup:

        prepareDataForSearchByTerm()
        reindexData()

        when:

        Set<SearchResultEntry> results = null
        List<Entity> dbResults = null

        UserSession userSession = authUser('constraintUser1')
        UserSession savedUserSession = userSessionSource.userSession
        ((TestUserSessionSource) userSessionSource).setUserSession(userSession)
        try {
            results = searchByIndex('fedor')
            dbResults = searchMainEntityByDb('fedor')
        } finally {
            ((TestUserSessionSource) userSessionSource).setUserSession(savedUserSession)
        }

        then:

        assertResults(results, dbResults)
    }

    def "search by related entity without security constrains"() {
        setup:

        prepareDataForSearchByRelatedEntity()
        reindexData()

        when:

        Set<SearchResultEntry> results = searchByIndex('fedor')
        List<Entity> dbResults = searchRelatedEntityByDb('fedor', false)

        then:

        assertResults(results, dbResults)
    }

    def "search by related entity with security constrains"() {
        setup:

        prepareDataForSearchByRelatedEntity()
        reindexData()

        when:

        Set<SearchResultEntry> results = null
        List<Entity> dbResults = null

        UserSession userSession = authUser('constraintUser1')
        UserSession savedUserSession = userSessionSource.userSession
        ((TestUserSessionSource) userSessionSource).setUserSession(userSession)

        try {
            results = searchByIndex('fedor')
            dbResults = searchRelatedEntityByDb('fedor', true)
        } finally {
            ((TestUserSessionSource) userSessionSource).setUserSession(savedUserSession)
        }

        then:

        assertResults(results, dbResults)
    }

    protected UserSession authUser(String login) {
        authenticationManager.login(
                new LoginPasswordCredentials(login, PASSWORD, Locale.getDefault()))
                .getSession()
    }

    void assertResults(Set<SearchResultEntry> indexResults, List<Entity> dbResults) {
        assert indexResults.size() == dbResults.size()
        for (Entity entity : dbResults) {
            boolean exists = indexResults.stream().anyMatch(new Predicate<SearchResultEntry>() {
                @Override
                boolean test(SearchResultEntry t) {
                    return Objects.equals(entity.id, t.entityInfo.id)
                }
            })
            assert exists
        }
    }

    void prepareDataForSearchByTerm() {
        Transaction tx = persistence.createTransaction()
        try {
            EntityManager em = persistence.entityManager
            int maxSize = 1000
            for (int i = 0; i < maxSize; i++) {
                MainEntity mainEntity = metadata.create(MainEntity)
                if (i % 3 == 0) {
                    mainEntity.setDescription("fedor")
                } else if (i % 3 == 1) {
                    mainEntity.setDescription("petr")
                } else if (i % 3 == 2) {
                    mainEntity.setDescription("timofei")
                }
                mainEntity.setName("name#" + i);
                mainEntity.setRls(Integer.toString(i % 2))
                em.persist(mainEntity)
            }
            tx.commit()
        } finally {
            tx.end()
        }
    }

    void prepareDataForSearchByRelatedEntity() {
        Transaction tx = persistence.createTransaction()
        List<RelatedEntity> related = new ArrayList<>();
        try {
            EntityManager em = persistence.entityManager
            int relatedMaxSize = 20
            for (int i = 0; i < relatedMaxSize; i++) {
                RelatedEntity relatedEntity = metadata.create(RelatedEntity.class)
                if (i % 3 == 0) {
                    relatedEntity.setDescription("fedor");
                } else if (i % 3 == 1) {
                    relatedEntity.setDescription("petr");
                } else if (i % 3 == 2) {
                    relatedEntity.setDescription("timofei");
                }
                relatedEntity.setName("name#" + i)
                relatedEntity.setRls(Integer.toString(i % 2))
                related.add(relatedEntity)
                em.persist(relatedEntity)
            }

            int maxSize = 1000;
            for (int i = 0; i < maxSize; i++) {
                MainEntity mainEntity = metadata.create(MainEntity.class)
                if (i % 3 == 0) {
                    mainEntity.setDescription("ivan")
                } else if (i % 3 == 1) {
                    mainEntity.setDescription("alex")
                } else if (i % 3 == 2) {
                    mainEntity.setDescription("tom")
                }
                mainEntity.setName("name#" + i);
                mainEntity.setRls(Integer.toString(i % 2));
                mainEntity.setRelation(related.get(i % related.size()))
                em.persist(mainEntity);
            }
            tx.commit()
        } finally {
            tx.end()
        }
    }


    void reindexData() {
        ftsManager.reindexAll()
        Integer count = null;
        while (count == null || count > 0) {
            count = ftsManager.processQueue()
        }
    }

    Set<SearchResultEntry> searchByIndex(String term) {
        SearchResult searchResult = null
        QueryKey queryKey = null
        Set<SearchResultEntry> results = new LinkedHashSet<>()
        while (searchResult == null || !searchResult.isEmpty()) {
            searchResult = ftsService.search(term, (QueryKey) queryKey)
            queryKey = searchResult.queryKey
            results.addAll(searchResult.getAllEntries())
        }
        return results
    }

    List<Entity> searchMainEntityByDb(String term) {
        LoadContext loadContext = new LoadContext(MainEntity)
        loadContext.setView(View.MINIMAL)
        loadContext.setQueryString('select e from ftstest$MainEntity e where e.description like :desc')
                .setParameter('desc', '%' + term + '%')
        return dataManager.secure().loadList(loadContext)
    }

    List<Entity> searchRelatedEntityByDb(String term, boolean useRls) {
        List<Entity> result = new ArrayList<>()
        LoadContext loadContext = new LoadContext(MainEntity)
        loadContext.setView(View.MINIMAL)
        if (useRls) {
            loadContext.setQueryString('select e from ftstest$MainEntity e where e.relation.description like :desc and e.relation.rls = \'1\'')
                    .setParameter('desc', '%' + term + '%')
        } else {
            loadContext.setQueryString('select e from ftstest$MainEntity e where e.relation.description like :desc')
                    .setParameter('desc', '%' + term + '%')
        }
        result.addAll(dataManager.loadList(loadContext))

        loadContext = new LoadContext(RelatedEntity)
        loadContext.setView(View.MINIMAL)
        loadContext.setQueryString('select e from ftstest$RelatedEntity e where e.description like :desc')
                .setParameter('desc', '%' + term + '%')
        result.addAll(dataManager.secure().loadList(loadContext))

        return result
    }

    protected void clearData() {
        new QueryRunner(persistence.getDataSource()).update("delete from FTS_TEST_MAIN_ENTITY")
        new QueryRunner(persistence.getDataSource()).update("delete from FTS_TEST_RELATED_ENTITY")
        new QueryRunner(persistence.getDataSource()).update("delete from SYS_FTS_QUEUE")
    }
}
