package com.example.cashcard;

public enum Role {
    CardOwner("CARD_OWNER"),
    NonCardOwner("NON_CARD_OWNER");

    private String role;
 
    Role(String role) {
        this.role = role;
    }
 
    public String getRole() {
        return this.role;
    }
}
