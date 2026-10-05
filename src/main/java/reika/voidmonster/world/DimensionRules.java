/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** API overrides take precedence over the configurable dimension list. */
public final class DimensionRules<K> {
    private final Map<K, Boolean> overrides = new HashMap<>();
    private final Set<K> configured = new HashSet<>();
    private boolean whitelist;

    public boolean allows(K dimension) {
        return overrides.getOrDefault(dimension, configured.contains(dimension) == whitelist);
    }

    public void override(K dimension, boolean allow) {
        overrides.put(dimension, allow);
    }

    public void configure(Collection<K> dimensions, boolean whitelist) {
        configured.clear();
        configured.addAll(dimensions);
        this.whitelist = whitelist;
    }

    public void addDimensions(Collection<K> dimensions) {
        configured.addAll(dimensions);
    }
}
