package com.eclipselink.analyzer.demo.l5.aggregate;

import javax.persistence.Embeddable;

@Embeddable
public class Holding {
    private String symbol;
    private int quantity;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
