package com.valiantech.core.iam.audit.model;



public record Metadata(String description, Object object) {
    public Metadata(String description) {
        this(description, null);
    }
}
