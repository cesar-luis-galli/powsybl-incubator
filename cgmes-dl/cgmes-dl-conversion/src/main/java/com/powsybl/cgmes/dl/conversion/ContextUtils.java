/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion;

import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.triplestore.api.TripleStore;

import java.util.Objects;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public final class ContextUtils {

    private ContextUtils() {
    }

    public static String contextNameFor(CgmesSubset subset, TripleStore tripleStore, String modelId) {
        Objects.requireNonNull(subset);
        Objects.requireNonNull(tripleStore);
        Objects.requireNonNull(modelId);
        String contextNameEQ = contextNameForEquipmentSubset(tripleStore);
        return contextNameEQ != null
                ? buildContextNameForSubsetFrom(contextNameEQ, subset)
                : modelId + "_" + subset.getIdentifier() + ".xml";
    }

    private static String contextNameForEquipmentSubset(TripleStore tripleStore) {
        String eq = CgmesSubset.EQUIPMENT.getIdentifier();
        String eqBD = CgmesSubset.EQUIPMENT_BOUNDARY.getIdentifier();
        for (String contextName : tripleStore.contextNames()) {
            if (contextName.contains(eq) && !contextName.contains(eqBD)) {
                return contextName;
            }
        }
        return null;
    }

    private static String buildContextNameForSubsetFrom(String contextNameEQ, CgmesSubset subset) {
        String eq = CgmesSubset.EQUIPMENT.getIdentifier();
        return contextNameEQ.replace(eq, subset.getIdentifier());
    }
}
