package net.ihiroky.niotty.codec.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hiroki Itoh
 */
public class HeaderFields {

    private Map<String, List<String>> fieldMap_;

    public HeaderFields() {
        fieldMap_ = new HashMap<String, List<String>>();
    }

    public void add(String name, String value) {
        List<String> valueList = fieldMap_.get(name);
        if (valueList != null) {
            valueList.add(value);
        } else {
            valueList = new ArrayList<String>(3);
            valueList.add(value);
            fieldMap_.put(name, valueList);
        }
    }

    public String value(String name) {
        List<String> valueList = fieldMap_.get(name);
        return (valueList != null) ? valueList.get(0) : null;
    }

    public List<String> values(String name) {
        List<String> valueList = fieldMap_.get(name);
        return (valueList != null)
                ? Collections.unmodifiableList(valueList)
                : Collections.<String>emptyList();
    }
}
