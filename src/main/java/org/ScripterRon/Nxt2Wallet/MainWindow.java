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
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        Number.class, Number.class, String.class};

    /** Transaction table column types */
    private static final int[] columnTypes = {
        SizedTable.DATE, SizedTable.ADDRESS, SizedTable.TYPE, SizedTable.ADDRESS,
        SizedTable.AMOUNT, SizedTable.AMOUNT, SizedTable.STATUS};

    /** Main window is minimized */
    private boolean windowMinimized = false;

    /** Account field */
    private final JLabel accountField;

    /** Account balance field */
    private final JLabel balanceField;

    /** Last block field */
    private final JLabel chainHeightField;

    /** Table count */
    private final int tableCount;

    /** Table map */
    private final Map<Integer, TransactionTableModel> tableMap = new HashMap<>();

    /** Tabbed pane containing the transaction tables */
    private final JTabbedPane tabbedPane;

    /** Transaction table */
    private final JTable[] table;

    /** Transaction table model */
    private final TransactionTableModel[] tableModel;

    /** Event handler thread */
    private Thread eventThread = null;

    /** Event handler shutdown started */
    private volatile boolean shutdown = false;

    /** Event handler token */
    private long eventToken;

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
        StringBuilder sb = new StringBuilder(64);
        sb.append(Utils.idToString(Main.accountId)).append(" / ").append(Main.accountRsId);
        if (Main.accountName.length() != 0)
            sb.append(" (").append(Main.accountName).append(")");
        accountField = new JLabel("<html><b>Account:   " + sb.toString() + "</b></html>", JLabel.CENTER);
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
        tableCount = Main.chains.size();
        table = new JTable[tableCount];
        tableModel = new TransactionTableModel[tableCount];
        tabbedPane = new JTabbedPane();
        int index = 0;
        for (Chain chain : Main.chains.values()) {
            tableModel[index] = new TransactionTableModel(columnNames, columnClasses, chain);
            table[index] = new SizedTable(tableModel[index], columnTypes);
            table[index].setRowSorter(new TableRowSorter<>(tableModel[index]));
            table[index].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(table[index]);
            JPanel tablePane = new JPanel(new BorderLayout());
            tablePane.setBackground(Color.WHITE);
            tablePane.add(scrollPane, BorderLayout.CENTER);
            tabbedPane.addTab(chain.getName(), tablePane);
            tableMap.put(chain.getId(), tableModel[index]);
            index++;
        }
        //
        // Create the button pane
        //
        ButtonPane buttonPane = new ButtonPane(this, 15, new String[] {"Send money", "send money"},
                                                         new String[] {"View exchange", "view exchange"},
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
        // "view exchange"      - View exchange orders
        //
        try {
            int tab;
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
                    tab = tabbedPane.getSelectedIndex();
                    if (tab < 0) {
                        JOptionPane.showMessageDialog(this, "No transaction pane is selected",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        SendCoinsDialog.showDialog(this, tableModel[tab].getChain());
                    }
                    break;
                case "view contacts":
                    ContactsDialog.showDialog(this);
                    for (TransactionTableModel model : tableModel)
                        model.fireTableDataChanged();
                    break;
                case "view exchange":
                    tab = tabbedPane.getSelectedIndex();
                    if (tab < 0) {
                        JOptionPane.showMessageDialog(this, "No transaction pane is selected",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        viewExchange(tableModel[tab].getChain());
                    }
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
     * View exchange orders
     *
     * @param   chain           Current chain
     */
    private void viewExchange(Chain chain) {
        try {
            Response response = Request.getCoinExchangeOrders(chain.getId());
            ExchangeDialog.showDialog(this, chain, response.getObjectList("orders"));
        } catch (IOException exc) {
            Main.log.error("Unable to get exchange orders", exc);
            Main.logException("Unable to get exchange orders", exc);
        } catch (Exception exc) {
            Main.log.error("Exception while viewing exchange orders", exc);
            Main.logException("Exception while viewing exchange orders", exc);
        }
    }

    /**
     * Change the Nxt account
     */
    private void changeAccount() {
        //
        // Get the account
        //
        long accountId = AccountDialog.showDialog(this);
        if (accountId == 0)
            return;
        //
        // Stop the event handler and process pending Swing events
        //
        stopEventHandler();
        SwingUtilities.invokeLater(() -> {
            //
            // Switch to the new account
            //
            try {
                int i = Main.accounts.indexOf(accountId);
                if (i >= 0) {
                    Main.passPhrase = Main.secretPhrases.get(i);
                } else {
                    Main.passPhrase = "";
                }
                Main.accountId = accountId;
                Main.accountRsId = Utils.getAccountRsId(accountId);
                Main.accountTransactions.clear();
                Main.unconfirmedTransactions.clear();
                Main.getAccount();
                for (TransactionTableModel model : tableModel) {
                    model.resetTransactions();
                }
                StringBuilder sb = new StringBuilder(64);
                sb.append(Utils.idToString(Main.accountId)).append(" / ").append(Main.accountRsId);
                if (Main.accountName.length() != 0)
                    sb.append(" (").append(Main.accountName).append(")");
                accountField.setText("<html><b>Account:   " + sb.toString() + "</b></html>");
                updateNodeStatus();
                startEventHandler();
            } catch (IOException exc) {
                Main.log.error("Unable to get initial account information", exc);
                Main.logException("Unable to get initial account information", exc);
            }
        });
    }

    /**
     * Start the Nxt event handler
     */
    private void startEventHandler() {
        shutdown = false;
        eventThread = new Thread(this, "Nxt Event Handler");
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
            Request.eventRegister(eventList, eventToken, false, true);
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
        Main.log.debug("Event handler started");
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
            eventList.add("Transaction.ADDED_CONFIRMED_TRANSACTIONS." + Main.accountRsId);
            eventList.add("Transaction.ADDED_UNCONFIRMED_TRANSACTIONS." + Main.accountRsId);
            eventList.add("Transaction.REMOVED_UNCONFIRMED_TRANSACTIONS." + Main.accountRsId);
            Response eventResponse = Request.eventRegister(eventList, 0, false, false);
            eventToken = eventResponse.getLong("token");
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
                List<Event> eventList = Request.eventWait(eventToken, 60);
                if (shutdown)
                    break;
                if (eventList.isEmpty())
                    continue;
                //
                // Process the events
                //
                Response response;
                for (Event event : eventList) {
                    switch (event.getName()) {
                        case "Block.BLOCK_PUSHED":
                            response = Request.getBlockchainStatus();
                            Main.blockHeight = response.getInt("numberOfBlocks") - 1;
                            SwingUtilities.invokeAndWait(() -> {
                                for (TransactionTableModel model : tableModel) {
                                    model.updateTransactionStatus();
                                }
                            });
                            break;
                        case "Transaction.ADDED_CONFIRMED_TRANSACTIONS":
                        case "Transaction.ADDED_UNCONFIRMED_TRANSACTIONS":
                            for (String eventId : event.getIds()) {
                                String[] eventParts = eventId.split(":");
                                if (eventParts.length != 2) {
                                    Main.log.error("Invalid transaction event id: " + eventId);
                                } else {
                                    final int txChainId = Integer.valueOf(eventParts[0]);
                                    byte[] fullHash = Utils.parseHexString(eventParts[1]);
                                    response = Request.getTransaction(fullHash, txChainId);
                                    final Transaction addedTx = new Transaction(response);
                                    SwingUtilities.invokeAndWait(() -> {
                                        tableMap.get(txChainId).addTransaction(addedTx);
                                    });
                                }
                            }
                            break;
                        case "Transaction.REMOVED_UNCONFIRMED_TRANSACTIONS":
                            for (String eventId : event.getIds()) {
                                String[] eventParts = eventId.split(":");
                                if (eventParts.length != 2) {
                                    Main.log.error("Invalid transaction event id: " + eventId);
                                } else {
                                    final int txChainId = Integer.valueOf(eventParts[0]);
                                    final byte[] fullHash = Utils.parseHexString(eventParts[1]);
                                    SwingUtilities.invokeAndWait(() -> {
                                        tableMap.get(txChainId).removeUnconfirmedTransaction(fullHash);
                                    });
                                }
                            }
                            break;
                        case "Block.BLOCK_POPPED":
                            final long popBlockId = Utils.stringToId(event.getIds().get(0));
                            SwingUtilities.invokeLater(() -> {
                                for (TransactionTableModel model : tableModel)
                                    model.popTransactions(popBlockId);
                            });
                            break;
                    }
                }
                //
                // Update the account balances
                //
                for (int chainId : Main.chains.keySet()) {
                    response = Request.getBalance(Main.accountId, chainId);
                    Main.accountBalance.put(chainId, response.getLong("unconfirmedBalanceNQT"));
                }
                SwingUtilities.invokeLater(() -> updateNodeStatus());
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
        Main.log.debug("Event handler stopped");
    }

    /**
     * Update the node status
     */
    private void updateNodeStatus() {
        chainHeightField.setText("<html><b>Chain height:   " + Main.blockHeight + "</b></html>");
        StringBuilder sb = new StringBuilder(64);
        sb.append("<html><b>Account balances:   ");
        boolean firstBalance = true;
        for (Chain chain : Main.chains.values()) {
            if (!firstBalance)
                sb.append(", ");
            sb.append(Utils.nqtToString(Main.accountBalance.get(chain.getId()), chain.getDecimals()))
                    .append(" ").append(chain.getName());
            firstBalance = false;
        }
        sb.append("</b></html>");
        balanceField.setText(sb.toString());
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

        /** Chain */
        private final Chain chain;

        /** Account transactions */
        private final List<Transaction> txList = new LinkedList<>();

        /** Account transaction map */
        private final Map<Long, Transaction> txMap = new HashMap<>();

        /**
         * Create the transaction table model
         *
         * @param       columnName          Column names
         * @param       columnClasses       Column classes
         * @param       chain               Chain
         */
        public TransactionTableModel(String[] columnNames, Class<?>[] columnClasses, Chain chain) {
            super();
            if (columnNames.length != columnClasses.length)
                throw new IllegalArgumentException("Number of names not same as number of classes");
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
            this.chain = chain;
            //
            // Build the initial transaction list
            //
            Main.accountTransactions.forEach(tx -> {
                if (tx.getChainId() == chain.getId()) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            Main.unconfirmedTransactions.forEach(tx -> {
                if (tx.getChainId() == chain.getId()) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            //
            // Sort the transaction by descending timestamp
            //
            txList.sort((o1, o2) -> {
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
                    else if (accountId != 0)
                        value = Utils.getAccountRsId(accountId);
                    else
                        value = "";
                    break;
                case 4:                                 // Amount
                    BigDecimal amount = new BigDecimal(tx.getAmount(), MathContext.DECIMAL128)
                            .movePointLeft(chain.getDecimals());
                    if (tx.getSenderId() == Main.accountId)
                        value = amount.negate();
                    else
                        value = amount;
                    break;
                case 5:                                 // Fee
                    BigDecimal fee = new BigDecimal(tx.getFee(), MathContext.DECIMAL128)
                            .movePointLeft(chain.getDecimals());
                    if (tx.getSenderId() == Main.accountId)
                        value = fee.negate();
                    else
                        value = fee;
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
         * Get the chain for this table
         *
         * @return              Chain
         */
        public Chain getChain() {
            return chain;
        }

        /**
         * Get the chain identifier for this table
         *
         * @return              Chain identifier
         */
        public int getChainId() {
            return chain.getId();
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
                if (tx.getChainId() == chain.getId()) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            Main.unconfirmedTransactions.forEach(tx -> {
                if (tx.getChainId() == chain.getId()) {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                }
            });
            //
            // Sort the transaction by descending timestamp
            //
            txList.sort((o1, o2) -> {
                int c = (o1.getTimestamp().compareTo(o2.getTimestamp()));
                return (c < 0 ? 1 : (c > 0 ? -1 : 0));
            });
            //
            // Notify listeners that the table data has changed
            //
            fireTableDataChanged();
        }

        /**
         * Add a confirmed account transaction
         *
         * @param       tx              Transaction
         */
        public void addTransaction(Transaction tx) {
            if (tx.getChainId() == chain.getId()) {
                Transaction listTx = txMap.get(tx.getId());
                if (listTx != null) {
                    if (listTx.getBlockId() != tx.getBlockId() && tx.getBlockId() != 0) {
                        listTx.setBlockId(tx.getBlockId());
                        listTx.setHeight(tx.getHeight());
                        fireTableDataChanged();
                    }
                } else {
                    txList.add(tx);
                    txMap.put(tx.getId(), tx);
                    txList.sort((o1, o2) -> {
                        int c = (o1.getTimestamp().compareTo(o2.getTimestamp()));
                        return (c < 0 ? 1 : (c > 0 ? -1 : 0));
                    });
                    fireTableDataChanged();
                }
            }
        }

        /**
         * Update transaction status
         */
        public void updateTransactionStatus() {
            for (int i=0; i<10; i++) {
                if (i == txList.size())
                    break;
                if (Main.blockHeight - txList.get(i).getHeight() == CONFIRM_COUNT) {
                    fireTableRowsUpdated(0, Math.min(txList.size()-1, 9));
                    break;
                }
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
            fireTableDataChanged();
        }

        /**
         * Remove unconfirmed transaction
         *
         * @param       fullHash        Transaction hash
         */
        public void removeUnconfirmedTransaction(byte[] fullHash) {
            long txId = Utils.fullHashToId(fullHash);
            Transaction tx = txMap.get(txId);
            if (tx != null && tx.getBlockId() == 0) {
                txList.remove(tx);
                txMap.remove(txId);
                fireTableDataChanged();
            }
        }
    }
}
