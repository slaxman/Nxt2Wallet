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

import java.io.IOException;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * SendCoinsDialog will create a new transaction to send coins to a specified recipient.
 */
public class SendCoinsDialog extends JDialog implements ActionListener {

    /** Address field */
    private final JComboBox<Object> addressField;

    /** Amount field */
    private final JTextField amountField;

    /** Fee field */
    private final JTextField feeField;

    /** Chain identifier */
    private final int chainId;

    /** Send address */
    private long sendAddress;

    /** Send amount */
    private long sendAmount = 0;

    /** Send fee */
    private long sendFee = 0;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     * @param       chainId         Chain identifier
     */
    public SendCoinsDialog(JFrame parent, int chainId) {
        super(parent, "Send Coins", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.chainId = chainId;
        //
        // Create the address field
        //
        if (Main.contactsList.isEmpty()) {
            addressField = new JComboBox<>();
        } else {
            String[] addrList = new String[Main.contactsList.size()];
            int index = 0;
            for (Contact contact : Main.contactsList)
                addrList[index++] = contact.getName();
            addressField = new JComboBox<>(addrList);
        }
        addressField.setEditable(true);
        addressField.setSelectedIndex(-1);
        addressField.setPreferredSize(new Dimension(340, 25));
        JPanel addressPane = new JPanel();
        addressPane.add(new JLabel("Address  ", JLabel.RIGHT));
        addressPane.add(addressField);
        //
        // Create the amount field
        //
        amountField = new JTextField("", 15);
        JPanel amountPane = new JPanel();
        amountPane.add(new JLabel("Amount  ", JLabel.RIGHT));
        amountPane.add(amountField);
        //
        // Create the fee field
        //
        feeField = new JTextField("1", 10);
        JPanel feePane = new JPanel();
        feePane.add(new JLabel("Fee  ", JLabel.RIGHT));
        feePane.add(feeField);
        //
        // Create the buttons (Send, Done)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Send", "send"},
                                                     new String[] {"Done", "done"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(addressPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(amountPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(feePane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the send dialog
     *
     * @param       parent              Parent frame
     * @param       chainId             Chain identifier
     */
    public static void showDialog(JFrame parent, int chainId) {
        try {
            SendCoinsDialog dialog = new SendCoinsDialog(parent, chainId);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } catch (Exception exc) {
            Main.logException("Exception while displaying dialog", exc);
        }
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param   ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "send"       - Send coins
        // "done"       - Done
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "send":
                    if (checkFields()) {
                        String confirmText = String.format("Do you want to send %s %s to %s?",
                                Utils.nqtToString(sendAmount),
                                Main.chains.get(chainId),
                                Utils.getAccountRsId(sendAddress));
                        if (JOptionPane.showConfirmDialog(this, confirmText, "Send Coins",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            if (sendCoins()) {
                                JOptionPane.showMessageDialog(this, "Coins sent to "
                                        + Utils.getAccountRsId(sendAddress), "Coins Sent",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(this, "Coins were not sent",
                                        "Coins Not Sent", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                    break;
                case "done":
                    setVisible(false);
                    dispose();
                    break;
            }
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Verify the fields
     *
     * @return                                  TRUE if the fields are valid
     */
    private boolean checkFields() {
        try {
            //
            // Get the send address
            //
            String sendString = (String)addressField.getSelectedItem();
            if (sendString == null) {
                JOptionPane.showMessageDialog(this, "You must enter a send address",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            int index = addressField.getSelectedIndex();
            if (index < 0) {
                sendString = sendString.toUpperCase().trim();
                if (sendString.startsWith("NXT-")) {
                    sendAddress = Utils.parseAccountRsId(sendString);
                } else {
                    sendAddress = Utils.stringToId(sendString);
                }
            } else {
                sendAddress = Main.contactsList.get(index).getAccountId();
            }
            //
            // Get the send amount
            //
            String amountString = amountField.getText().trim();
            if (amountString.isEmpty()) {
                JOptionPane.showMessageDialog(this, "You must enter the amount to send",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            sendAmount = Utils.stringToNQT(amountString);
            //
            // Get the fee amount
            //
            String feeString = feeField.getText().trim();
            if (feeString.isEmpty()) {
                JOptionPane.showMessageDialog(this, "You must enter a transaction fee",
                                              "Enter", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            sendFee = Utils.stringToNQT(feeString);
            //
            // Check account balance
            //
            if (sendAmount + sendFee > Main.accountBalance.get(chainId)) {
                JOptionPane.showMessageDialog(this, "You do not have enough " + Main.chains.get(chainId),
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException exc) {
            JOptionPane.showMessageDialog(this, "Amount or fee is not a valid number", "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IdentifierException exc) {
            JOptionPane.showMessageDialog(this, "Send address is not valid", "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Create and broadcast the transaction
     *
     * @return                      TRUE if the coins were sent
     */
    private boolean sendCoins() {
        //
        // Get the sender secret phrase
        //
        String secretPhrase;
        if (Main.passPhrase.length() == 0) {
            secretPhrase = JOptionPane.showInputDialog(this, "Enter your secret phrase");
            if (secretPhrase == null || secretPhrase.length() == 0)
                return false;
        } else {
            secretPhrase = Main.passPhrase;
        }
        //
        // Create the transaction
        //
        boolean broadcasted = false;
        try {
            byte[] publicKey = Crypto.getPublicKey(secretPhrase);
            Response response = Request.sendMoney(sendAddress, chainId, sendAmount, sendFee, publicKey);
            byte[] txBytes = response.getHexString("unsignedTransactionBytes");
            Transaction tx = new Transaction(txBytes);
            if (tx.getRecipientId() != sendAddress || tx.getAmount() != sendAmount ||
                    tx.getFee() != sendFee || tx.getSenderId() != Main.accountId) {
                JOptionPane.showMessageDialog(this, "Transaction returned by Nxt node is not valid",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            byte[] signature = Crypto.sign(txBytes, secretPhrase);
            System.arraycopy(signature, 0, txBytes, Transaction.SIGNATURE_OFFSET, 64);
            Request.broadcastTransaction(txBytes);
            broadcasted = true;
        } catch (KeyException exc) {
            Main.log.error("Unable to get public key from secret phrase", exc);
            Main.logException("Unable to get public key from secret phrase", exc);
        } catch (IOException exc) {
            Main.log.error("Unable to send coins", exc);
            Main.logException("Unable to send coins", exc);
        } catch (Exception exc) {
            Main.log.error("Exception while sending coins", exc);
            Main.logException("Exception while sending coins", exc);
        }
        return broadcasted;
    }
}
