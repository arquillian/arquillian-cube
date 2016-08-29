package org.arquillian.cube.kubernetes.impl.await;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public class CompositeCondition implements Callable<Boolean> {

    private final List<Callable<Boolean>> callableList;

    public CompositeCondition(Collection<Callable<Boolean>> callables) {
        this.callableList = new ArrayList<>(callables);
    }

    public CompositeCondition(Callable<Boolean>... callables) {
        this.callableList = Arrays.asList(callables);
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        for (int i = 0; i < callableList.size() && result; i++) {
            result = result && callableList.get(i).call();

        }
        return result;
    }
}
