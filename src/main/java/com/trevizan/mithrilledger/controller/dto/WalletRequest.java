package com.trevizan.mithrilledger.controller.dto;

public record WalletRequest(
    String ownerId,
    String currency
) { }
