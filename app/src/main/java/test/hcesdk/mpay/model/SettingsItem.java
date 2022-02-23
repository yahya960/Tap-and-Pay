package test.hcesdk.mpay.model;

import java.util.ArrayList;
import java.util.List;

public class SettingsItem {

    private String label;
    private String value;
    private SettingsItemType type;

    private List<String> possibleValues=new ArrayList<>();

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SettingsItemType getType() {
        return type;
    }

    public void setType(SettingsItemType type) {
        this.type = type;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(List<String> possibleValues) {
        this.possibleValues = possibleValues;
    }
}
