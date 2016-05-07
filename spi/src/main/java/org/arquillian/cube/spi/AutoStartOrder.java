package org.arquillian.cube.spi;

import java.util.List;

public interface AutoStartOrder<T> {

    List<String[]> getAutoStartOrder(T config);
    List<String[]> getAutoStopOrder(T config);

}
