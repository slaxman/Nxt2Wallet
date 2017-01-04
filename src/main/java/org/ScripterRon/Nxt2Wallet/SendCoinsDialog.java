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
    private final JComboBox<String> addressField;

    /** Amount field */
    private final JTextField amountField;

    /** Fee field */
    private final JTextField feeField;

    /** Exchange rate field */
    private final JTextField rateField;

    /** Chain */
    private final Chain chain;

    /** Send address */
    private long sendAddress;

    /** Send amount */
    private long sendAmount = 0;

    /** Send fee */
    private long sendFee = 0;

    /** Send rate */
    private long sendRate = 0;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     * @param       chain           Chain
     */
    public SendCoinsDialog(JFrame parent, Chain chain) {
        super(parent, "Send " + chain.getName(), Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.chain = chain;
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
        // Create the bundler exchange rate field
        //
        rateField = new JTextField("", 10);
        JPanel ratePane = new JPanel();
        ratePane.add(new JLabel("Exchange rate  ", JLabel.RIGHT));
        ratePane.add(rateField);
        //
        // Create the fee field
        //
        feeField = new JTextField("", 10);
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
        if (chain.getId() != Main.fxtChainId) {
            Long rate = Main.bundlerRates.get(chain.getId());
            if (rate != null)
                rateField.setText(Utils.nqtToString(rate, chain.getDecimals()));
            contentPane.add(ratePane);
            contentPane.add(Box.createVerticalStrut(15));
        }
        contentPane.add(feePane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the send dialog
     *
     * @param       parent              Parent frame
     * @param       chain               Chain
     */
    public static void showDialog(JFrame parent, Chain chain) {
        try {
            SendCoinsDialog dialog = new SendCoinsDialog(parent, chain);
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
                        if (sendCoins()) {
                            JOptionPane.showMessageDialog(this, chain.getName()
                                    + " sent to " + Utils.getAccountRsId(sendAddress),
                                    "Coins Sent", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    chain.getName() + " was not sent",
                                    "Coins Not Sent", JOptionPane.INFORMATION_MESSAGE);
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
            sendAmount = Utils.stringToNQT(amountField.getText().trim(), chain.getDecimals());
            if (sendAmount <= 0) {
                JOptionPane.showMessageDialog(this, "You must enter the amount to send",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            //
            // Get the fee amount and the exchange rate
            //
            sendFee = Utils.stringToNQT(feeField.getText().trim(), chain.getDecimals());
            sendRate = Utils.stringToNQT(rateField.getText().trim(), chain.getDecimals());
            if (sendFee == 0 && sendRate == 0 && chain.getId() != Main.fxtChainId) {
                JOptionPane.showMessageDialog(this,
                        "You must enter either a fee or a rate for a child chain transaction",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (ArithmeticException exc) {
            JOptionPane.showMessageDialog(this, "Too many decimal digits specified", "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (NumberFormatException exc) {
            JOptionPane.showMessageDialog(this, "Numeric value is not valid", "Error",
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
            Main.passPhrase = secretPhrase;
        } else {
            secretPhrase = Main.passPhrase;
        }
        //
        // Create the transaction
        //
        boolean broadcasted = false;
        try {
            byte[] publicKey = Crypto.getPublicKey(secretPhrase);
            Response response = Request.sendMoney(sendAddress, chain.getId(),
                    sendAmount, sendFee, sendRate, publicKey);
            byte[] txBytes = response.getHexString("unsignedTransactionBytes");
            Transaction tx = new Transaction(txBytes);
            if (sendFee == 0)
                sendFee = tx.getFee();
            if (tx.getRecipientId() != sendAddress || tx.getAmount() != sendAmount ||
                    tx.getFee() != sendFee || tx.getSenderId() != Main.accountId) {
                JOptionPane.showMessageDialog(this, "Transaction returned by Nxt node is not valid",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String confirmText = String.format("Do you want to send %s %s with fee %s to %s?",
                            Utils.nqtToString(sendAmount, chain.getDecimals()), chain.getName(),
                            Utils.nqtToString(sendFee, chain.getDecimals()),
                            Utils.getAccountRsId(sendAddress));
            if (JOptionPane.showConfirmDialog(this, confirmText, "Send Coins",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
                return false;
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
