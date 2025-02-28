package com.paypal.raptor.aiml.model;

import java.util.List;

public class NestedDataOutput {

    private String name;
    private List<Integer> shape;
    private String datatype;
    private List<String> data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getShape() {
        return shape;
    }

    public void setShape(List<Integer> shape) {
        this.shape = shape;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }
}
