package com.Omnibus.application.port.in;

import com.Omnibus.application.dto.TransferCommand;
import com.Omnibus.application.dto.TransferResult;

/**
 * Use-case port: execute a money transfer between two accounts.
 */
public interface CreateTransferUseCase {

    TransferResult execute(TransferCommand command);
}
