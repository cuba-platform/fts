/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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