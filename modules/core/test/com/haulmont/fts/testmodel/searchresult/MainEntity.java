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

package com.haulmont.fts.testmodel.searchresult;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;

@NamePattern("%s|name")
@Table(name = "FTS_TEST_MAIN_ENTITY")
@Entity(name = "ftstest$MainEntity")
public class MainEntity extends StandardEntity {
    private static final long serialVersionUID = 5678796433436432442L;

    @Column(name = "NAME")
    protected String name;

    @Column(name = "RLS")
    protected String rls;

    @Column(name = "DESCRIPTION")
    protected String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RELATION_ID")
    protected RelatedEntity relation;

    public void setRls(String rls) {
        this.rls = rls;
    }

    public String getRls() {
        return rls;
    }


    public void setRelation(RelatedEntity relation) {
        this.relation = relation;
    }

    public RelatedEntity getRelation() {
        return relation;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}