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
import javax.swing.WindowConstants;

/**
 * Display the account selection dialog
 */
public class AccountDialog extends JDialog implements ActionListener {

    /** Account list field */
    private final JComboBox<String> accountField;

    /** Account */
    private long accountId = 0;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     */
    public AccountDialog(JFrame parent) {
        super(parent, "Select Account", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Create the account field
        //
        String[] accountList = new String[Main.accounts.size()];
        int index = 0;
        for (Long id : Main.accounts) {
            accountList[index++] = Utils.getAccountRsId(id);
        }
        accountField = new JComboBox<>(accountList);
        accountField.setEditable(true);
        accountField.setSelectedIndex(-1);
        accountField.setPreferredSize(new Dimension(340, 25));
        JPanel accountPane = new JPanel();
        accountPane.add(new JLabel("Account  ", JLabel.RIGHT));
        accountPane.add(accountField);
        //
        // Create the buttons (OK, Cancel)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"OK", "ok"},
                                                     new String[] {"Cancel", "cancel"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(accountPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the account selection dialog
     *
     * @param       parent              Parent frame
     * @return                          Server connection or null
     */
    public static long showDialog(JFrame parent) {
        long accountId = 0;
        try {
            AccountDialog dialog = new AccountDialog(parent);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
            accountId = dialog.accountId;
        } catch (Exception exc) {
            Main.log.error("Exception while displaying dialog", exc);
            Main.logException("Exception while displaying dialog", exc);
        }
        return accountId;
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param   ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "OK"         - Switch to the selected account
        // "cancel"     - Cancel the request
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "ok":
                    String account = (String)accountField.getSelectedItem();
                    if (account != null)
                        account = account.toUpperCase().trim();
                    if (account == null || account.length() == 0) {
                        JOptionPane.showMessageDialog(this, "No account selected", "Error",
                                                      JOptionPane.ERROR_MESSAGE);
                    } else {
                        if (account.startsWith("NXT-")) {
                            accountId = Utils.parseAccountRsId(account);
                        } else {
                            accountId = Long.parseUnsignedLong(account);
                        }
                    }
                    setVisible(false);
                    dispose();
                    break;
                case "cancel":
                    setVisible(false);
                    dispose();
                    break;
            }
        } catch (NumberFormatException | IdentifierException exc) {
            JOptionPane.showMessageDialog(this, "Invalid account", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }
}
