package com.finpay.account.dto;

import com.finpay.account.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAccountStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;
}
