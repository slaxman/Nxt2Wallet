/*
 * Copyright 2016 Ronald W Hoffman.
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

import org.ScripterRon.Nxt2API.IdentifierException;
import org.ScripterRon.Nxt2API.Utils;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Contact contains the name and Nxt address used to send funds
 */
public class Contact {

    /** Contact name */
    private String name;

    /** Account identifier */
    private long accountId;

    /** Account Reed-Solomon identifier */
    private String accountRsId;

    /**
     * Create a new contact
     *
     * @param       name                    Contact name
     * @param       address                 Contact address (Account identifier or Reed-Solomon address)
     * @throws      IdentifierException     Invalid account identifier
     */
    public Contact(String name, String address) throws IdentifierException {
        this.name = name;
        String addr = address.toUpperCase().trim();
        if (addr.startsWith("NXT-")) {
            accountId = Utils.parseAccountRsId(addr);
            accountRsId = addr;
        } else {
            accountId = Utils.stringToId(addr);
            accountRsId = Utils.getAccountRsId(accountId);
        }
    }

    /**
     * Create a new contact from a serialized byte stream
     *
     * @param       inStream                Serialized byte stream
     * @throws      EOFException            End-of-data while processing serialized stream
     * @throws      IdentifierException     Invalid account identifier
     * @throws      IOException             Unable to read serialized stream
     */
    public Contact(InputStream inStream) throws EOFException, IdentifierException, IOException {
        //
        // Decode the contact name
        //
        int length = Utils.decodeInteger(inStream);
        byte[] nameBytes = new byte[length];
        int count = inStream.read(nameBytes);
        if (count != length)
            throw new EOFException("End-of-data while processing serialized contact");
        name = new String(nameBytes, "UTF-8");
        //
        // Get the account
        //
        byte[] accountBytes = new byte[8];
        count = inStream.read(accountBytes);
        if (count != 8)
            throw new EOFException("End-of-data while processing serialized contact");
        accountId = ((long)accountBytes[0] & 0xff) | (((long)accountBytes[1] & 0xff) << 8) |
                    (((long)accountBytes[2] & 0xff) <<16) | (((long)accountBytes[3] & 0xff) <<24) |
                    (((long)accountBytes[4] & 0xff) <<32) | (((long)accountBytes[5] & 0xff) <<40) |
                    (((long)accountBytes[6] & 0xff) <<48) | (((long)accountBytes[7] & 0xff) <<56);
        accountRsId = Utils.getAccountRsId(accountId);
    }

    /**
     * Return the contact name
     *
     * @return                              Contact name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the contact name
     *
     * @param       name                    Contact name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the contact account identifier
     *
     * @param       accountId               Account identifier
     */
    public void setAccountId(long accountId) {
        this.accountId = accountId;
        accountRsId = Utils.getAccountRsId(accountId);
    }

    /**
     * Return the account identifier
     *
     * @return                              Account identifier
     */
    public long getAccountId() {
        return accountId;
    }

    /**
     * Return the account Reed-Solomon identifier
     *
     * @return                              Reed-Solomon account identifier
     */
    public String getAccountRsId() {
        return accountRsId;
    }

    /**
     * Serialize the contact and write it to the supplied output stream
     *
     * @param       outStream               Output stream
     * @throws      IOException             Unable to create serialized stream
     */
    public void getBytes(OutputStream outStream) throws IOException {
        byte[] nameBytes = name.getBytes("UTF-8");
        byte[] nameLength = Utils.encodeInteger(nameBytes.length);
        byte[] accountBytes = new byte[8];
        for (int i=0; i<8; i++)
            accountBytes[i] = (byte)(accountId >>> (i*8));
        outStream.write(nameLength);
        outStream.write(nameBytes);
        outStream.write(accountBytes);
    }

    /**
     * Return the hash code for the contact
     *
     * @return                              Hash code
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Compare two contacts
     *
     * Two contacts are equal if they have the same name
     *
     * @param       obj                     Contact to compare
     * @return                              TRUE if the contacts are equal
     */
    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof Contact) && name.equals(((Contact)obj).name));
    }
}
