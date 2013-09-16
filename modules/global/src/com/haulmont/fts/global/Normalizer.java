/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.global;

import java.util.List;

/**
 * @author tsarevskiy
 * @version $Id$
 */
public interface Normalizer {

    String getAnyNormalForm(String word);

    List<String> getAllNormalForms(String word);

}
