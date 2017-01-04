/*
 * Copyright 2017 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Wallet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Coin exchange order issue attachment
 */
public class OrderIssueAttachment {

    /** Chain */
    private final Chain chain;

    /** Exchange chain */
    private final Chain exchangeChain;

    /** Quantity */
    private final long quantity;

    /** Price */
    private final long price;

    /**
     * Create the order issue attachment
     *
     * @param   response                    Coin exchange transaction response
     * @throws  IllegalArgumentException    Response is not valid
     * @throws  NumberFormatException       Invalid numeric value
     */
    public OrderIssueAttachment(Response response) throws IllegalArgumentException, NumberFormatException {
        int chainId = response.getInt("chain");
        chain = Main.chains.get(chainId);
        if (chain == null)
            throw new IllegalArgumentException("Chain " + chainId + " is not valid");
        chainId = response.getInt("exchangeChain");
        exchangeChain = Main.chains.get(chainId);
        if (exchangeChain == null)
            throw new IllegalArgumentException("Exchange chain " + chainId + " is not valid");
        quantity = response.getLong("quantityQNT");
        price = response.getLong("priceNQT");
    }

    /**
     * Create the order issue attachment
     *
     * @param   transactionBytes            Transaction bytes
     * @throws  BufferUnderflowException    End-of-data reached parsing attachment
     * @throws  IllegalArgumentException    Invalid attachment
     */
    public OrderIssueAttachment(byte[] transactionBytes)
                                            throws BufferUnderflowException, IllegalArgumentException {
        ByteBuffer buffer = ByteBuffer.wrap(transactionBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(Transaction.BASE_LENGTH);
        int version = buffer.get();
        if (version != 1)
            throw new IllegalArgumentException("Attachment version " + version + " is not supported");
        int chainId = buffer.getInt();
        chain = Main.chains.get(chainId);
        if (chain == null)
            throw new IllegalArgumentException("Chain " + chainId + " is not valid");
        chainId = buffer.getInt();
        exchangeChain = Main.chains.get(chainId);
        if (exchangeChain == null)
            throw new IllegalArgumentException("Exchange chain " + chainId + " is not valid");
        quantity = buffer.getLong();
        price = buffer.getLong();
    }

    /**
     * Get the chain
     *
     * @return                      Chain
     */
    public Chain getChain() {
        return chain;
    }

    /**
     * Get the exchange chain
     *
     * @return                      Exchange chain
     */
    public Chain getExchangeChain() {
        return exchangeChain;
    }

    /**
     * Get the quantity
     *
     * @return                      Quantity
     */
    public long getQuantity() {
        return quantity;
    }

    /**
     * Get the price
     *
     * @return                      Price
     */
    public long getPrice() {
        return price;
    }
}
