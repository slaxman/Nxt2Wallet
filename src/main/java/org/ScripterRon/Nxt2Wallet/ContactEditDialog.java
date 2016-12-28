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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Dialog to create a new contact or edit an existing contact
 */
public class ContactEditDialog extends JDialog implements ActionListener {

    /** Contact to edit */
    private final Contact editContact;

    /** Updated contact */
    private Contact updatedContact;

    /** Name field */
    private final JTextField nameField;

    /** Address field */
    private final JTextField addressField;

    /**
     * Create the contact edit dialog
     *
     * @param       parent          Parent dialog
     * @param       contact         Contact to edit or null for a new contact
     */
    public ContactEditDialog(JDialog parent, Contact contact) {
        super(parent, "Edit Contact", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        editContact = contact;
        //
        // Create the edit pane
        //
        //    Name:         <text-field>
        //    Address:      <text-field>
        //
        JPanel editPane = new JPanel();
        editPane.setLayout(new BoxLayout(editPane, BoxLayout.X_AXIS));

        nameField = new JTextField(contact != null ? contact.getName() : "", 32);
        addressField = new JTextField(contact != null ? contact.getAccountRsId() : "", 24);

        JPanel namePane = new JPanel();
        namePane.add(new JLabel("Name:", JLabel.RIGHT));
        namePane.add(nameField);
        editPane.add(namePane);

        editPane.add(Box.createHorizontalStrut(10));

        JPanel addressPane = new JPanel();
        addressPane.add(new JLabel("Address:", JLabel.RIGHT));
        addressPane.add(addressField);
        editPane.add(addressPane);
        //
        // Create the buttons (Save, Cancel)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Save", "save"},
                                                     new String[] {"Cancel", "cancel"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(editPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the contact dialog
     *
     * @param       parent              Parent dialog
     * @param       contact             Contact to edit or null for a new contact
     * @return                          Updated contact or null if edit canceled
     */
    public static Contact showDialog(JDialog parent, Contact contact) {
        Contact updatedContact = null;
        try {
            ContactEditDialog dialog = new ContactEditDialog(parent, contact);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
            updatedContact = dialog.updatedContact;
        } catch (Exception exc) {
            Main.logException("Exception while displaying dialog", exc);
        }
        return updatedContact;
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param   ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "save"   - Save the table entry
        // "cancel" - Cancel the edit
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "save":
                    if (processFields()) {
                        setVisible(false);
                        dispose();
                    }
                    break;
                case "cancel":
                    updatedContact = null;
                    setVisible(false);
                    dispose();
                    break;
            }
        } catch (Exception exc) {
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Process the name and address fields
     *
     * @return      TRUE if the fields are valid
     */
    private boolean processFields() {
        String name = nameField.getText();
        String address = addressField.getText();
        //
        // Make sure we have name and address values
        //
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You must specify a name", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (name.length() > 64) {
            JOptionPane.showMessageDialog(this, "The name must be 64 characters or less", "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (address.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You must specify an address", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        //
        // Create a new contact
        //
        Contact newContact;
        try {
            newContact = new Contact(name, address);
        } catch (IdentifierException exc) {
            JOptionPane.showMessageDialog(this, "The address is not a valid Nxt account identifier",
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        //
        // Check for a duplicate contact
        //
        if (editContact == null || !editContact.getName().equals(newContact.getName())) {
            if (Main.contactsList.contains(newContact)) {
                JOptionPane.showMessageDialog(this, "Contact name already exists", "Error",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (editContact == null || editContact.getAccountId() != newContact.getAccountId()) {
            if (Main.contactsMap.get(newContact.getAccountId()) != null) {
                JOptionPane.showMessageDialog(this, "Contact address already exists", "Error",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        updatedContact = newContact;
        return true;
    }
}
