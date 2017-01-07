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

import org.ScripterRon.Nxt2API.Attachment;
import org.ScripterRon.Nxt2API.Chain;
import org.ScripterRon.Nxt2API.Crypto;
import org.ScripterRon.Nxt2API.KeyException;
import org.ScripterRon.Nxt2API.Nxt;
import org.ScripterRon.Nxt2API.Response;
import org.ScripterRon.Nxt2API.Transaction;
import org.ScripterRon.Nxt2API.Utils;

import java.io.IOException;
import java.util.Collection;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * ExchangeCoinsDialog will create a new transaction to exchange coins
 */
public class ExchangeCoinsDialog extends JDialog implements ActionListener, ItemListener {

    /** Chain field */
    private final JComboBox<String> chainField;

    /** Amount field */
    private final JTextField amountField;

    /** Price field */
    private final JTextField priceField;

    /** Fee field */
    private final JTextField feeField;

    /** Exchange rate field */
    private final JTextField rateField;

    /** Chain */
    private final Chain chain;

    /** Exchange chain */
    private Chain exchangeChain;

    /** Exchange amount */
    private long exchangeAmount = 0;

    /** Exchange price */
    private long exchangePrice = 0;

    /** Exchange fee */
    private long exchangeFee = 0;

    /** Exchange rate */
    private long exchangeRate = 0;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     * @param       chain           Chain
     */
    public ExchangeCoinsDialog(JDialog parent, Chain chain) {
        super(parent, "Exchange " + chain.getName(), Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.chain = chain;
        //
        // Create the chain field
        //
        Collection<Chain> chains = Nxt.getAllChains();
        final String[] chainList = new String[chains.size() - 1];
        int index = 0;
        for (Chain mapChain : chains) {
            if (mapChain != chain)
                chainList[index++] = mapChain.getName();
        }
        chainField = new JComboBox<>(chainList);
        chainField.addItemListener(this);
        chainField.setSelectedIndex(0);
        JPanel chainPane = new JPanel();
        chainPane.add(new JLabel("Chain  ", JLabel.RIGHT));
        chainPane.add(chainField);
        //
        // Create the amount field
        //
        amountField = new JTextField("", 15);
        JPanel amountPane = new JPanel();
        amountPane.add(new JLabel("Amount  ", JLabel.RIGHT));
        amountPane.add(amountField);
        //
        // Create the price field
        //
        priceField = new JTextField("", 15);
        JPanel pricePane = new JPanel();
        pricePane.add(new JLabel("Price  ", JLabel.RIGHT));
        pricePane.add(priceField);
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
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Exchange", "exchange"},
                                                     new String[] {"Done", "done"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(chainPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(amountPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(pricePane);
        contentPane.add(Box.createVerticalStrut(15));
        if (!chain.getName().equals(Nxt.FXT_CHAIN)) {
            Long rate = Main.bundlerRates.get(chain.getId());
            if (rate != null)
                rateField.setText(Utils.nqtToString(rate, chain.getDecimals()));
            contentPane.add(ratePane);
            contentPane.add(Box.createVerticalStrut(15));
            if (((String)chainField.getSelectedItem()).equals(Nxt.FXT_CHAIN))
                rateField.setEnabled(false);
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
    public static void showDialog(JDialog parent, Chain chain) {
        try {
            ExchangeCoinsDialog dialog = new ExchangeCoinsDialog(parent, chain);
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
        // "exchange"   - Exchange coins
        // "done"       - Done
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "exchange":
                    if (checkFields()) {
                        if (exchangeCoins()) {
                            JOptionPane.showMessageDialog(this,
                                    "Order to exchange " + chain.getName()
                                    + " for " + exchangeChain.getName() + " submitted",
                                    "Order Submitted", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Exchange order was not submitted",
                                    "Order Not Submitted", JOptionPane.INFORMATION_MESSAGE);
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
     * Item state changed (ItemListener interface)
     *
     * @param   ie              Item event
     */
    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            String name = (String)chainField.getSelectedItem();
            if (name.equals(Nxt.FXT_CHAIN)) {
                rateField.setEnabled(false);
            } else {
                rateField.setEnabled(true);
            }
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
            // Get the exchange chain
            //
            String name = (String)chainField.getSelectedItem();
            if (name == null) {
                JOptionPane.showMessageDialog(this, "You must select a chain for the exchange",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            for (Chain mapChain : Nxt.getAllChains()) {
                if (mapChain.getName().equals(name)) {
                    exchangeChain = mapChain;
                    break;
                }
            }
            //
            // Get the exchange amount
            //
            exchangeAmount = Utils.stringToNQT(amountField.getText().trim(), chain.getDecimals());
            if (exchangeAmount <= 0) {
                JOptionPane.showMessageDialog(this, "You must enter the amount to exchange",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            //
            // Get the exchange price
            //
            exchangePrice = Utils.stringToNQT(priceField.getText().trim(), chain.getDecimals());
            if (exchangePrice <= 0) {
                JOptionPane.showMessageDialog(this, "You must enter the exchange price",
                                                "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            //
            // Get the fee amount and the exchange rate
            //
            exchangeFee = Utils.stringToNQT(feeField.getText().trim(), chain.getDecimals());
            exchangeRate = Utils.stringToNQT(rateField.getText().trim(), chain.getDecimals());
            if (chain.getName().equals(Nxt.FXT_CHAIN) || exchangeChain.getName().equals(Nxt.FXT_CHAIN)) {
                exchangeRate = 0;
            } else if (exchangeFee == 0 && exchangeRate == 0) {
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
        }
        return true;
    }

    /**
     * Create and broadcast the transaction
     *
     * @return                      TRUE if the exchange order was sent
     */
    private boolean exchangeCoins() {
        //
        // Get the secret phrase
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
            Response response = Nxt.exchangeCoins(chain, exchangeChain,
                    exchangeAmount, exchangePrice, exchangeFee, exchangeRate, publicKey);
            byte[] txBytes = response.getHexString("unsignedTransactionBytes");
            Transaction tx = new Transaction(txBytes);
            Attachment.ExchangeOrderIssueAttachment attachment =
                    (Attachment.ExchangeOrderIssueAttachment)tx.getAttachment();
            if (exchangeFee == 0)
                exchangeFee = tx.getFee();
            if (tx.getFee() != exchangeFee || tx.getSenderId() != Main.accountId ||
                    attachment.getChain() != chain || attachment.getExchangeChain() != exchangeChain ||
                    attachment.getQuantity() != exchangeAmount || attachment.getPrice() != exchangePrice) {
                JOptionPane.showMessageDialog(this, "Transaction returned by Nxt node is not valid",
                                              "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            Chain txChain = (chain.getName().equals(Nxt.FXT_CHAIN) ||
                                exchangeChain.getName().equals(Nxt.FXT_CHAIN) ?
                                Nxt.getChain(Nxt.FXT_CHAIN) : chain);
            String confirmText = String.format("Do you want to exchange %s %s for %s with %s %s fee?",
                    Utils.nqtToString(exchangeAmount, chain.getDecimals()),
                    chain.getName(), exchangeChain.getName(),
                    Utils.nqtToString(exchangeFee, txChain.getDecimals()), txChain.getName());
            if (JOptionPane.showConfirmDialog(this, confirmText, "Exchange Coins",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
                return false;
            Nxt.broadcastTransaction(txBytes, secretPhrase);
            broadcasted = true;
        } catch (KeyException exc) {
            Main.log.error("Unable to sign transaction", exc);
            Main.logException("Unable to sign transaction", exc);
        } catch (IOException exc) {
            Main.log.error("Unable to exchange coins", exc);
            Main.logException("Unable to exchange coins", exc);
        } catch (Exception exc) {
            Main.log.error("Exception while exchanging coins", exc);
            Main.logException("Exception while exchanging coins", exc);
        }
        return broadcasted;
    }
}
