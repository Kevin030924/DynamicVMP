package org.framework.comparator;

import org.domain.PhysicalMachine;

import java.util.Comparator;

public class BestComparator implements Comparator<PhysicalMachine> {

    public BestComparator() {
    }

    @Override
    public int compare(final PhysicalMachine pm1, final PhysicalMachine pm2) {

        return pm1.getWeight().compareTo(pm2.getWeight());

    }
}
