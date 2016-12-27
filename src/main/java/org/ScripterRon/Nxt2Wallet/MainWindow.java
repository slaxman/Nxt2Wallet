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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Main application window
 */
public class MainWindow extends JFrame implements ActionListener, Runnable {

    /** Number of blocks required to confirm a transaction */
    private static final int CONFIRM_COUNT = 5;

    /** Transaction table column names */
    private static final String[] columnNames = {
        "Date", "Transaction ID", "Type", "Account", "Amount", "Fee", "Status"};

    /** Transaction table column classes */
    private static final Class<?>[] columnClasses = {
        Date.class, String.class, String.class, String.class,
        Long.class, Long.class, String.class};

    /** Transaction table column types */
    private static final int[] columnTypes = {
        SizedTable.DATE, SizedTable.ADDRESS, SizedTable.TYPE, SizedTable.ADDRESS, SizedTable.AMOUNT,
        SizedTable.AMOUNT, SizedTable.STATUS};

    /** Table count */
    private int tableCount;

    /** Chain mapping: chainId -> tableIndex */
    private final Map<Integer, Integer> chainMap = new HashMap<>();

    /** Table mapping: TableIndex -> chainId */
    private final Map<Integer, Integer> tableMap = new HashMap<>();

    /** Main window is minimized */
    private boolean windowMinimized = false;

    /** Account field */
    private final JLabel accountField;

    /** Account balance field */
    private final JLabel balanceField;

    /** Last block field */
    private final JLabel chainHeightField;

    /** Transaction table */
    private final JTable[] table;

    /** Transaction table model */
    private final TransactionTableModel[] tableModel;

    /** Event handler shutdown started */
    private volatile boolean shutdown = false;

    /**
     * Create the application window
     */
    public MainWindow() {
        //
        // Create the frame
        //
        super("Nxt2 Wallet");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Position the window using the saved position from the last time
        // the program was run
        //
        int frameX = 320;
        int frameY = 10;
        String propValue = Main.properties.getProperty("window.main.position");
        if (propValue != null) {
            int sep = propValue.indexOf(',');
            frameX = Integer.parseInt(propValue.substring(0, sep));
            frameY = Integer.parseInt(propValue.substring(sep+1));
        }
        setLocation(frameX, frameY);
        //
        // Size the window using the saved size from the last time
        // the program was run
        //
        int frameWidth = 640;
        int frameHeight = 580;
        propValue = Main.properties.getProperty("window.main.size");
        if (propValue != null) {
            int sep = propValue.indexOf(',');
            frameWidth = Math.max(frameWidth, Integer.parseInt(propValue.substring(0, sep)));
            frameHeight = Math.max(frameHeight, Integer.parseInt(propValue.substring(sep+1)));
        }
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        //
        // Create the application menu bar
        //
        JMenuBar menuBar = new JMenuBar();
        menuBar.setOpaque(true);
        menuBar.setBackground(new Color(230,230,230));
        //
        // Add the "File" menu to the menu bar
        //
        // The "File" menu contains "Change Account" and "Exit"
        //
        menuBar.add(new Menu(this, "File", new String[] {"Change Account", "change account"},
                                           new String[] {"Exit", "exit"}));
        //
        // Add the "Help" menu to the menu bar
        //
        // The "Help" menu contains "About"
        //
        menuBar.add(new Menu(this, "Help", new String[] {"Abount", "about"}));
        //
        // Add the menu bar to the window frame
        //
        setJMenuBar(menuBar);
        //
        // Create the account pane
        //
        String accountIdString = Utils.idToString(Main.accountId) + " / " + Main.accountRsId;
        accountField = new JLabel("<html><b>Account:   " + accountIdString + "</b></html>", JLabel.CENTER);
        balanceField = new JLabel("<html><b>Balances:   </b></html>", JLabel.CENTER);
        chainHeightField = new JLabel("<html><b>Chain height</b></html>", JLabel.CENTER);
        JPanel accountPane = new JPanel();
        accountPane.setLayout(new BoxLayout(accountPane, BoxLayout.Y_AXIS));
        accountPane.setOpaque(true);
        accountPane.setBackground(Color.WHITE);
        accountPane.add(accountField);
        accountPane.add(balanceField);
        accountPane.add(chainHeightField);
        accountPane.add(Box.createVerticalStrut(20));
        //
        // Create the transaction tables
        //
        tableCount = 0;
        Set<Integer> chainSet = Main.chains.keySet();
        for (Integer chainId : chainSet) {
            chainMap.put(chainId, tableCount);
            tableMap.put(tableCount, chainId);
            tableCount++;
        }
        table = new JTable[tableCount];
        tableModel = new TransactionTableModel[tableCount];
        JTabbedPane tabbedPane = new JTabbedPane();
        for (int i = 0; i < tableCount; i++) {
            tableModel[i] = new TransactionTableModel(columnNames, columnClasses, tableMap.get(i));
            table[i] = new SizedTable(tableModel[i], columnTypes);
            table[i].setRowSorter(new TableRowSorter<>(tableModel[i]));
            table[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(table[i]);
            JPanel tablePane = new JPanel(new BorderLayout());
            tablePane.setBackground(Color.WHITE);
            tablePane.add(scrollPane, BorderLayout.CENTER);
            tabbedPane.addTab(Main.chains.get(tableMap.get(i)), tablePane);
        }
        //
        // Create the button pane
        //
        ButtonPane buttonPane = new ButtonPane(this, 15, new String[] {"Send money", "send money"},
                                                         new String[] {"View contacts", "view contacts"});
        buttonPane.setOpaque(true);
        buttonPane.setBackground(Color.WHITE);
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setOpaque(true);
        contentPane.setBackground(Color.WHITE);
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPane.add(accountPane, BorderLayout.NORTH);
        contentPane.add(tabbedPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        setContentPane(contentPane);
        //
        // Receive WindowListener events
        //
        addWindowListener(new ApplicationWindowListener(this));
        //
        // Display the initial node status
        //
        updateNodeStatus();
        //
        // Start our event handler
        //
        startEventHandler();
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param       ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "about"              - Display information about this program
        // "change account"     - Change the Nxt account
        // "exit"               - Exit the program
        // "send nxt"           - Send Nxt
        // "view contacts"      - View contacts
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "about":
                    aboutNxtWallet();
                    break;
                case "change account":
                    changeAccount();
                    break;
                case "exit":
                    exitProgram();
                    break;
                case "send money":
                    //SendNxtDialog.showDialog(this);
                    //tableModel.updateTransactions(chainHeight);
                    break;
                case "view contacts":
                    //ContactsDialog.showDialog(this);
                    //tableModel.fireTableDataChanged();
                    break;
            }
        } catch (Exception exc) {
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Exit the application
     */
    private void exitProgram() {
        //
        // Stop our event handler
        //
        stopEventHandler();
        //
        // Remember the current window position and size unless the window
        // is minimized
        //
        if (!windowMinimized) {
            Point p = getLocation();
            Dimension d = getSize();
            Main.properties.setProperty("window.main.position", p.x+","+p.y);
            Main.properties.setProperty("window.main.size", d.width+","+d.height);
        }
        //
        // All done
        //
        Main.shutdown();
    }

    /**
     * Change the Nxt account
     */
    private void changeAccount() {

    }

    /**
     * Start the Nxt event handler
     */
    private void startEventHandler() {
        Thread eventThread = new Thread(this, "Nxt Event Handler");
        eventThread.setDaemon(true);
        eventThread.start();
    }

    /**
     * Stop the Nxt event handler
     */
    private void stopEventHandler() {
        shutdown = true;
        //
        // Cancel our event listener (this will cause the event wait to complete)
        //
        try {
            List<String> eventList = new ArrayList<>();
            Request.eventRegister(eventList, false, true);
        } catch (IOException exc) {
            Main.log.error("Unable to cancel event listener", exc);
            Main.logException("Unable to cancel event listener", exc);
        }
    }

    /**
     * Process server events
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        //
        // Get the initial server status
        //
        try {
            //
            // Register our events
            //
            List<String> eventList = new ArrayList<>();
            eventList.add("Block.BLOCK_PUSHED");
            eventList.add("Block.BLOCK_POPPED");
            Request.eventRegister(eventList, false, false);
        } catch (IOException exc) {
            Main.log.error("Unable to register our events", exc);
            Main.logException("Unable to register our events", exc);
            shutdown = true;
        } catch (Exception exc) {
            Main.log.error("Exception while registering server events", exc);
            Main.logException("Exception while registering server events", exc);
            shutdown = true;
        }
        //
        // Process server events
        //
        while (!shutdown) {
            try {
                //
                // Wait for an event
                //
                List<Event> eventList = Request.eventWait(60);
                if (shutdown)
                    break;
                if (eventList.isEmpty())
                    continue;
                //
                // Process the events
                //
                Response response;
                for (Event event : eventList) {
                    String eventId = event.getIds().get(0);
                    switch (event.getName()) {
                        case "Block.BLOCK_PUSHED":
                            response = Request.getBlockchainStatus();
                            Main.blockHeight = response.getInt("numberOfBlocks") - 1;
                            for (int index=0; ; index+=5) {
                                final Response[] txResponses = new Response[tableModel.length];
                                final Boolean[] txMatches = new Boolean[tableModel.length];
                                for (int i=0; i<tableModel.length; i++) {
                                    TransactionTableModel model = tableModel[i];
                                    txResponses[i] = Request.getBlockchainTransactions(
                                            Main.accountId, model.getChainId(), index, index+4);
                                }
                                SwingUtilities.invokeAndWait(() -> {
                                    for (int j=0; j<tableModel.length; j++) {
                                        TransactionTableModel model = tableModel[j];
                                        List<Map<String, Object>> mapList =
                                                txResponses[j].getObjectList("transactions");
                                        if (mapList.isEmpty()) {
                                            txMatches[j] = true;
                                        } else {
                                            List<Transaction> txList =
                                                    Transaction.processTransactions(mapList);
                                            txMatches[j] = model.addTransactions(txList);
                                        }
                                    }
                                });
                                boolean matched = true;
                                for (Boolean match : txMatches) {
                                    if (!match) {
                                        matched = false;
                                        break;
                                    }
                                }
                                if (matched)
                                    break;
                                }
                            break;
                        case "Block.BLOCK_POPPED":
                            final long popBlockId = Utils.stringToId(eventId);
                            SwingUtilities.invokeAndWait(() -> {
                                for (TransactionTableModel model : tableModel)
                                    model.popTransactions(popBlockId);
                            });
                            break;
                    }
                }
                SwingUtilities.invokeAndWait(() -> updateNodeStatus());
            } catch (InterruptedException | InvocationTargetException exc) {
                Main.log.error("Unable to perform status update", exc);
                Main.logException("Unable to perform status update", exc);
                shutdown = true;
            } catch (IOException exc) {
                Main.log.error("Unable to process server event", exc);
                Main.logException("Unable to process server event", exc);
                shutdown = true;
            } catch (Exception exc) {
                Main.log.error("Exception while processing server event", exc);
                Main.logException("Exception while processing server event", exc);
            }
        }
    }

    /**
     * Update the node status
     */
    private void updateNodeStatus() {
        try {
            Response response;
            chainHeightField.setText("<html><b>Chain height:   " + Main.blockHeight + "</b></html>");
            StringBuilder sb = new StringBuilder(64);
            sb.append("<html><b>Account balances:   ");
            boolean firstBalance = true;
            for (int i=0; i<tableCount; i++) {
                int chainId = tableMap.get(i);
                response = Request.getBalance(Main.accountId, chainId);
                String balance = Utils.nqtToString(response.getLong("unconfirmedBalanceNQT"));
                if (!firstBalance)
                    sb.append(", ");
                sb.append(balance).append(" ").append(Main.chains.get(chainId));
                firstBalance = false;
            }
            sb.append("</b></html>");
            balanceField.setText(sb.toString());
        } catch (IOException exc) {
            Main.log.error("Unable to issue Nxt API request", exc);
            Main.logException("Unable to issue Nxt API request", exc);
        }
    }

    /**
     * Display information about the Nxt2Wallet application
     */
    private void aboutNxtWallet() {
        StringBuilder info = new StringBuilder(256);
        info.append(String.format("<html>%s Version %s<br>%s Version %s<br>",
                    Main.applicationName, Main.applicationVersion, Main.nxtApplication, Main.nxtVersion));

        info.append("<br>User name: ");
        info.append(System.getProperty("user.name"));

        info.append("<br>Home directory: ");
        info.append(System.getProperty("user.home"));

        info.append("<br><br>OS: ");
        info.append(System.getProperty("os.name"));

        info.append("<br>OS version: ");
        info.append(System.getProperty("os.version"));

        info.append("<br>OS patch level: ");
        info.append(System.getProperty("sun.os.patch.level"));

        info.append("<br><br>Java vendor: ");
        info.append(System.getProperty("java.vendor"));

        info.append("<br>Java version: ");
        info.append(System.getProperty("java.version"));

        info.append("<br>Java home directory: ");
        info.append(System.getProperty("java.home"));

        info.append("<br>Java class path: ");
        info.append(System.getProperty("java.class.path"));

        info.append("<br><br>Current Java memory usage: ");
        info.append(String.format("%,.3f MB", (double)Runtime.getRuntime().totalMemory()/(1024.0*1024.0)));

        info.append("<br>Maximum Java memory size: ");
        info.append(String.format("%,.3f MB", (double)Runtime.getRuntime().maxMemory()/(1024.0*1024.0)));

        info.append("</html>");
        JOptionPane.showMessageDialog(this, info.toString(), "About NxtWallet",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Listen for window events
     */
    private class ApplicationWindowListener extends WindowAdapter {

        /**
         * Create the window listener
         *
         * @param       window      The application window
         */
        public ApplicationWindowListener(JFrame window) {
        }

        /**
         * Window has been minimized (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowIconified(WindowEvent we) {
            windowMinimized = true;
        }

        /**
         * Window has been restored (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowDeiconified(WindowEvent we) {
            windowMinimized = false;
        }

        /**
         * Window is closing (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowClosing(WindowEvent we) {
            exitProgram();
        }
    }

    /**
     * Transaction table model
     */
    private class TransactionTableModel extends AbstractTableModel {

        /** Column names */
        private final String[] columnNames;

        /** Column classes */
        private final Class<?>[] columnClasses;

        /** Chain identifier */
        private final int chainId;

        /** Account transactions */
        private final List<Transaction> txList = new LinkedList<>();

        /** Account transaction map */
        private final Map<Long, Transaction> txMap = new HashMap<>();

        /**
         * Create the transaction table model
         *
         * @param       columnName          Column names
         * @param       columnClasses       Column classes
         * @param       chainId             Chain identifier
         */
        public TransactionTableModel(String[] columnNames, Class<?>[] columnClasses, int chainId) {
            super();
            if (columnNames.length != columnClasses.length)
                throw new IllegalArgumentException("Number of names not same as number of classes");
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
            this.chainId = chainId;
            //
            // Build the initial transaction list
            //
            Main.accountTransactions.forEach(tx -> {
                if (tx.getChainId() == chainId) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            Main.unconfirmedTransactions.forEach(tx -> {
                if (tx.getChainId() == chainId) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            //
            // Sort the transaction by descending timestamp
            //
            Collections.sort(txList, (o1, o2) -> {
                int c = (o1.getTimestamp().compareTo(o2.getTimestamp()));
                return (c < 0 ? 1 : (c > 0 ? -1 : 0));
            });
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
            return txList.size();
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
            if (row >= txList.size())
                throw new IndexOutOfBoundsException("Table row "+row+" is not valid");
            Object value;
            Transaction tx = txList.get(row);
            //
            // Get the value for the requested cell
            //
            switch (column) {
                case 0:                                 // Date
                    value = tx.getTimestamp();
                    break;
                case 1:                                 // Transaction ID
                    value = Utils.idToString(tx.getId());
                    break;
                case 2:                                 // Type
                    value = tx.getTransactionType();
                    break;
                case 3:                                 // Account
                    long accountId = tx.getSenderId();
                    if (accountId == Main.accountId)
                        accountId = tx.getRecipientId();
                    Contact contact = Main.contactsMap.get(accountId);
                    if (contact != null)
                        value = contact.getName();
                    else
                        value = Utils.getAccountRsId(accountId);
                    break;
                case 4:                                 // Amount
                    if (tx.getSenderId() == Main.accountId)
                        value = -tx.getAmount();
                    else
                        value = tx.getAmount();
                    break;
                case 5:                                 // Fee
                    if (tx.getSenderId() == Main.accountId)
                        value = -tx.getFee();
                    else
                        value = tx.getFee();
                    break;
                case 6:                                 // Confirmations
                    int height = tx.getHeight();
                    if (height == 0) {
                        value = "Pending";
                    } else if (Main.blockHeight - height < CONFIRM_COUNT) {
                        value = "Building";
                    } else {
                        value = "Confirmed";
                    }
                    break;
                default:
                    throw new IndexOutOfBoundsException("Table column "+column+" is not valid");
            }
            return value;
        }

        /**
         * Get the chain identifier for this table
         */
        public int getChainId() {
            return chainId;
        }

        /**
         * Reset the account transactions
         */
        public void resetTransactions() {
            txList.clear();
            txMap.clear();
            //
            // Build the initial transaction list
            //
            Main.accountTransactions.forEach(tx -> {
                if (tx.getChainId() == chainId) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            Main.unconfirmedTransactions.forEach(tx -> {
                if (tx.getChainId() == chainId) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            //
            // Sort the transaction by descending timestamp
            //
            Collections.sort(txList, (o1, o2) -> {
                int c = (o1.getTimestamp().compareTo(o2.getTimestamp()));
                return (c < 0 ? 1 : (c > 0 ? -1 : 0));
            });
            //
            // Notify listeners that the table data has changed
            //
            fireTableDataChanged();
        }

        /**
         * Add account transactions
         *
         * @param       transactions    Transaction list
         * @return                      TRUE if the transaction list is synchronized
         */
        public boolean addTransactions(List<Transaction> transactions) {
            boolean matched = false;
            boolean modified = false;
            boolean updated = false;
            for (Transaction tx : transactions) {
                if (tx.getChainId() == chainId) {
                    Transaction listTx = txMap.get(tx.getId());
                    if (listTx != null) {
                        if (listTx.getBlockId() == tx.getBlockId()) {
                            matched = true;
                            if (Main.blockHeight - listTx.getHeight() == CONFIRM_COUNT)
                                updated = true;
                        } else {
                            listTx.setBlockId(tx.getBlockId());
                            listTx.setHeight(tx.getHeight());
                        }
                    } else {
                        txList.add(tx);
                        txMap.put(tx.getId(), tx);
                        modified = true;
                    }
                }
            }
            if (modified) {
                Collections.sort(txList, (o1, o2) -> {
                    int c = (o1.getTimestamp().compareTo(o2.getTimestamp()));
                    return (c < 0 ? 1 : (c > 0 ? -1 : 0));
                });
                fireTableDataChanged();
            } else if (updated) {
                fireTableRowsUpdated(0, Math.min(txList.size()-1, 4));
            }
            return matched;
        }

        /**
         * Add a new unconfirmed account transaction (assumed to be newest transaction)
         *
         * @param       tx              Transaction
         */
        public void addUnconfirmedTransaction(Transaction tx) {
            if (tx.getChainId() == chainId) {
                txList.add(0, tx);
                txMap.put(tx.getId(), tx);
                fireTableRowsInserted(0, 0);
            }
        }

        /**
         * Mark transactions in popped block as unconfirmed
         *
         * @param       blockId         Block identifier
         */
        public void popTransactions(long blockId) {
            txList.forEach(tx -> {
                if (tx.getBlockId() == blockId) {
                    tx.setBlockId(0);
                    tx.setHeight(0);
                }
            });
        }
    }
}
