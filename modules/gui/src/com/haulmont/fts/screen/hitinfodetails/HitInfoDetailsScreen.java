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

package com.haulmont.fts.screen.hitinfodetails;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.HitInfo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UiController("fts_HitInfoDetailsScreen")
@UiDescriptor("hit-info-details-screen.xml")
public class HitInfoDetailsScreen extends Screen {

    @Inject
    protected FtsService ftsService;

    @Inject
    protected VBoxLayout hitInfoBox;

    @Inject
    protected UiComponents uiComponents;

    protected List<HitInfo> hitInfos = new ArrayList<>();

    protected Entity entity;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buildUi();
    }

    protected void buildUi() {
        for (HitInfo hitInfo : hitInfos) {
            BoxLayout hitInfoLayout = createHitInfoLayout(hitInfo);
            hitInfoBox.add(hitInfoLayout);
        }
    }

    protected BoxLayout createHitInfoLayout(HitInfo hitInfo) {
        HBoxLayout layout = uiComponents.create(HBoxLayout.class);
        layout.setSpacing(true);
        String propertyCaption = ftsService.getHitPropertyCaption(entity.getMetaClass().getName(), hitInfo.getFieldName());
        Label<String> captionLabel = uiComponents.create(Label.TYPE_STRING);
        captionLabel.setValue(propertyCaption + ":");
        layout.add(captionLabel);

        Label<String> hitInfoDetailsLabel = uiComponents.create(Label.TYPE_STRING);
        hitInfoDetailsLabel.setHtmlEnabled(true);
        hitInfoDetailsLabel.setValue(hitInfo.getHighlightedText());
        layout.add(hitInfoDetailsLabel);
        return layout;
    }

    public void setHitInfos(List<HitInfo> hitInfos) {
        this.hitInfos = hitInfos;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }
}
