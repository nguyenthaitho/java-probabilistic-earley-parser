package org.leibnizcenter.cfg.util;

import java.util.*;

/**
 * Dumb implementation of a multimap
 * Created by maarten on 21-1-17.
 */
public class MyMultimap<T, T1> {
    private Map<T, Set<T1>> map = new HashMap<>();
    private Set<T1> values = new HashSet<>();
    private boolean isLocked = false;

    public Collection<T1> get(T el) {
        if (map.containsKey(el)) return map.get(el);
        else return null;
    }

    public void put(T k, T1 v) {
        final Set<T1> s;
        if (map.containsKey(k)) {
            s = map.get(k);
        } else {
            s = new HashSet<>();
            map.put(k, s);
        }
        s.add(v);
        values.add(v);
    }

    public boolean containsKey(T s) {
        return map.containsKey(s);
    }

    public boolean lock() {
        map = Collections.unmodifiableMap(map);
        values = Collections.unmodifiableSet(values);

        boolean wasLocked = isLocked;
        isLocked = true;
        return wasLocked;
    }

    public Collection<T1> values() {
        return values;
    }

}
