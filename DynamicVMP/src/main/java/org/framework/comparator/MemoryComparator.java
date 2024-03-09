package org.framework.comparator;

import org.domain.VirtualMachine;

import java.util.Comparator;

/**
 * 比较{@link VirtualMachine}的内存的比较器。
 */
public class MemoryComparator implements Comparator<VirtualMachine> {

    private final Boolean largerValuesPreferred;

    // 构造函数
    public MemoryComparator() {

        this.largerValuesPreferred = true;
    }

    // 默认构造函数
    public MemoryComparator(Boolean largerValuesPreferred) {

        this.largerValuesPreferred = largerValuesPreferred;
    }
    /**
     * 比较两个虚拟机的内存大小。
     *
     * @param vm1 第一个虚拟机
     * @param vm2 第二个虚拟机
     * @return 负值、零或正值，表示vm1的内存小于、等于或大于vm2的内存
     */
    @Override
    public int compare(VirtualMachine vm1, VirtualMachine vm2) {

        if (largerValuesPreferred) {
            return Double.compare(vm1.getResources().get(1), vm2.getResources().get(1)) * -1;
        } else {
            return Double.compare(vm1.getResources().get(1), vm2.getResources().get(1));
        }

    }
}
