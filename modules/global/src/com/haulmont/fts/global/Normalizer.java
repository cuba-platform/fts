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
