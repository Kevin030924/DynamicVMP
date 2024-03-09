package org.framework.comparator;

import org.domain.PhysicalMachine;

import java.util.Comparator;

public class WorstComparator implements Comparator<PhysicalMachine> {

    public WorstComparator() {
    }

    @Override
    public int compare(final PhysicalMachine pm1, final PhysicalMachine pm2) {

        return pm2.getWeight().compareTo(pm1.getWeight());
    }
}
