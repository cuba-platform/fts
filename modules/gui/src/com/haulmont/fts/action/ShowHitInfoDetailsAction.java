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

package com.haulmont.fts.action;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.actions.ListAction;
import com.haulmont.cuba.gui.components.filter.FtsFilterHelper;
import com.haulmont.fts.app.HitInfoLoaderService;
import com.haulmont.fts.global.HitInfo;
import com.haulmont.fts.screen.hitinfodetails.HitInfoDetailsScreen;

import java.util.List;

/**
 * An action that is added to the table or dataGrid components after full text search is performed using the generic filter component. The action
 * opens a window with hit info details (in which fields the search term was found). This action is useful if hit infos tooltips generation is
 * disabled.
 */
public class ShowHitInfoDetailsAction extends ListAction {

    protected String searchTerm;

    public ShowHitInfoDetailsAction(String searchTerm) {
        super(FtsFilterHelper.FTS_DETAILS_ACTION_ID);
        this.searchTerm = searchTerm;
    }

    @Override
    public String getCaption() {
        return AppBeans.get(Messages.class).getMessage(ShowHitInfoDetailsAction.class, "showHitInfoDetailsAction.caption");
    }

    @Override
    public void actionPerform(Component component) {
        Entity entity = target.getSingleSelected();
        if (entity == null) return;
        HitInfoLoaderService hitInfoLoaderService = AppBeans.get(HitInfoLoaderService.class);
        List<HitInfo> hitInfos = hitInfoLoaderService.loadHitInfos(entity.getMetaClass().getName(), entity.getId(), searchTerm);
        HitInfoDetailsScreen hitInfoDetailsScreen = AppBeans.get(ScreenBuilders.class)
                .screen(target.getFrame().getFrameOwner())
                .withScreenClass(HitInfoDetailsScreen.class)
                .build();
        hitInfoDetailsScreen.setHitInfos(hitInfos);
        hitInfoDetailsScreen.setEntity(entity);
        hitInfoDetailsScreen.show();
    }
}
