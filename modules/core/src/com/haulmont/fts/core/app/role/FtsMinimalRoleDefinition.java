/*
 * Copyright (c) 2008-2020 Haulmont.
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

package com.haulmont.fts.core.app.role;

import com.haulmont.cuba.security.app.role.AnnotatedRoleDefinition;
import com.haulmont.cuba.security.app.role.annotation.Role;
import com.haulmont.cuba.security.app.role.annotation.ScreenAccess;
import com.haulmont.cuba.security.role.ScreenPermissionsContainer;

/**
 * System role that grants minimal permissions required for all users for FTS.
 */
@Role(name = FtsMinimalRoleDefinition.ROLE_NAME, isDefault = false)
public class FtsMinimalRoleDefinition extends AnnotatedRoleDefinition {

    public static final String ROLE_NAME = "system-fts-minimal";

    @Override
    @ScreenAccess(screenIds = {
            "ftsSearchResults",
            "fts_HitInfoDetailsScreen"
    })
    public ScreenPermissionsContainer screenPermissions() {
        return super.screenPermissions();
    }

    @Override
    public String getLocName() {
        return "FTS Minimal";
    }
}
