package com.ryzz3nn.woidzquests.models;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PurchaseLimit {
    private final LocalDateTime firstPurchaseTime;
    private int purchaseCount;
    
    public PurchaseLimit(LocalDateTime firstPurchaseTime, int purchaseCount) {
        this.firstPurchaseTime = firstPurchaseTime;
        this.purchaseCount = purchaseCount;
    }
    
    public PurchaseLimit(LocalDateTime firstPurchaseTime) {
        this.firstPurchaseTime = firstPurchaseTime;
        this.purchaseCount = 1;
    }
    
    public void incrementPurchases() {
        this.purchaseCount++;
    }
}
