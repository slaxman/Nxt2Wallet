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

import java.util.Collections;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * ContactsDialog displays a table containing names and associated addresses.  The user
 * can create a new entry, edit an entry or delete an entry.
 *
 * A contact address represents a Nxt account identifier and is used to send and receive
 * Nxt.
 */
public class ContactsDialog extends JDialog implements ActionListener {

    /** Address table column classes */
    private static final Class<?>[] columnClasses = {String.class, String.class};

    /** Address table column names */
    private static final String[] columnNames = {"Name", "Address"};

    /** Address table column types */
    private static final int[] columnTypes = {SizedTable.NAME, SizedTable.ADDRESS};

    /** Contact table model */
    private final ContactTableModel tableModel;

    /** Address table */
    private final JTable table;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     */
    public ContactsDialog(JFrame parent) {
        super(parent, "Contacts", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Create the address table
        //
        tableModel = new ContactTableModel(columnNames, columnClasses);
        table = new SizedTable(tableModel, columnTypes);
        table.setRowSorter(new TableRowSorter<>(tableModel));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //
        // Create the table scroll pane
        //
        JScrollPane scrollPane = new JScrollPane(table);
        //
        // Create the table pane
        //
        JPanel tablePane = new JPanel();
        tablePane.setBackground(Color.WHITE);
        tablePane.add(scrollPane);
        //
        // Create the buttons (New, Copy, Edit, Delete, Done)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"New", "new"},
                                                     new String[] {"Copy ID", "copy"},
                                                     new String[] {"Edit", "edit"},
                                                     new String[] {"Delete", "delete"},
                                                     new String[] {"Done", "done"});
        buttonPane.setBackground(Color.white);
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.setBackground(Color.WHITE);
        contentPane.add(tablePane);
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the dialog
     *
     * @param       parent              Parent frame
     */
    public static void showDialog(JFrame parent) {
        try {
            JDialog dialog = new ContactsDialog(parent);
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
        // "new"   - Create a new address entry
        // "copy"   - Copy an address to the system clipbboard
        // "edit"   - Edit an address entry
        // "delete" - Delete an address entry
        // "done"   - All done
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "done":
                    setVisible(false);
                    dispose();
                    break;
                case "new":
                    editContact(null, -1);
                    break;
                default:
                    int row = table.getSelectedRow();
                    if (row < 0) {
                        JOptionPane.showMessageDialog(this, "No entry selected", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        row = table.convertRowIndexToModel(row);
                        Contact contact = Main.contactsList.get(row);
                        switch (action) {
                            case "copy":
                                StringSelection sel = new StringSelection(contact.getAccountRsId());
                                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                                cb.setContents(sel, null);
                                break;
                            case "edit":
                                editContact(contact, row);
                                break;
                            case "delete":
                                Main.contactsList.remove(row);
                                Main.contactsMap.remove(contact.getAccountId());
                                Main.saveContacts();
                                tableModel.fireTableRowsDeleted(row, row);
                                break;
                        }
                    }
            }
        } catch (Exception exc) {
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Edit the contact
     *
     * @param       contact                 Contact or null for a new contact
     * @param       row                     Table row or -1 if the key is not in the table
     * @throws      IdentifierException     Invalid account identifier
     */
    private void editContact(Contact contact, int row) throws IdentifierException {
        //
        // Show the contact edit dialog and get the updated contact
        //
        Contact updatedContact = ContactEditDialog.showDialog(this, contact);
        if (updatedContact == null)
            return;
        String name = updatedContact.getName();
        //
        // Update the existing contact if we just changed the contact address
        //
        if (contact != null && name.equals(contact.getName())) {
            Main.contactsMap.remove(contact.getAccountId());
            Main.contactsMap.put(updatedContact.getAccountId(), contact);
            Main.contactsList.get(row).setAccountId(updatedContact.getAccountId());
            Main.saveContacts();
            tableModel.fireTableRowsUpdated(row, row);
            return;
        }
        //
        // Remove an existing contact
        //
        if (contact != null) {
            Main.contactsList.remove(row);
            Main.contactsMap.remove(contact.getAccountId());
        }
        //
        // Insert the contact into the contact list sorted by name
        //
        Main.contactsList.add(updatedContact);
        Main.contactsMap.put(updatedContact.getAccountId(), updatedContact);
        Collections.sort(Main.contactsList, (o1, o2) -> {
            return o1.getName().compareTo(o2.getName());
        });
        Main.saveContacts();
        tableModel.fireTableDataChanged();
    }

    /**
     * ContactTableModel is the table model for the contact dialog
     */
    private class ContactTableModel extends AbstractTableModel {

        /** Column names */
        private String[] columnNames;

        /** Column classes */
        private Class<?>[] columnClasses;

        /**
         * Create the table model
         *
         * @param       columnNames     Column names
         * @param       columnClasses   Column classes
         */
        public ContactTableModel(String[] columnNames, Class<?>[] columnClasses) {
            super();
            if (columnNames.length != columnClasses.length)
                throw new IllegalArgumentException("Number of names not same as number of classes");
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
        }

        /**
         * Get the number of columns in the table
         *
         * @return                  The number of columns
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Get the column class
         *
         * @param       column      Column number
         * @return                  The column class
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return columnClasses[column];
        }

        /**
         * Get the column name
         *
         * @param       column      Column number
         * @return                  Column name
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Get the number of rows in the table
         *
         * @return                  The number of rows
         */
        @Override
        public int getRowCount() {
            return Main.contactsList.size();
        }

        /**
         * Get the value for a cell
         *
         * @param       row         Row number
         * @param       column      Column number
         * @return                  Returns the object associated with the cell
         */
        @Override
        public Object getValueAt(int row, int column) {
            if (row >= Main.contactsList.size())
                throw new IndexOutOfBoundsException("Table row "+row+" is not valid");
            Object value;
            Contact contact = Main.contactsList.get(row);
            switch (column) {
                case 0:
                    value = contact.getName();
                    break;
                case 1:
                    value = contact.getAccountRsId();
                    break;
                default:
                    throw new IndexOutOfBoundsException("Table column "+column+" is not valid");
            }
            return value;
        }
    }
}
