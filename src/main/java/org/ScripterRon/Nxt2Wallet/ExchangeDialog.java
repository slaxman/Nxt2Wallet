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

import org.ScripterRon.Nxt2API.Chain;
import org.ScripterRon.Nxt2API.Nxt;
import org.ScripterRon.Nxt2API.Response;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/**
 * ExchangeDialog displays a table of exchange offers a coin and allows
 * the user to issue a matching exchange order.
 */
public class ExchangeDialog extends JDialog implements ActionListener {

    /** Exchange table column names */
    private static final String[] columnNames = {"Coin", "Account", "Amount", "Price"};

    /** Exchange table column classes */
    private static final Class<?>[] columnClasses = {String.class, String.class,
        Number.class, Number.class};

    /** Exchange table column types */
    private static final int[] columnTypes = {SizedTable.CHAIN, SizedTable.ADDRESS,
        SizedTable.AMOUNT, SizedTable.AMOUNT};

    /** Exchange table model */
    private final ExchangeTableModel tableModel;

    /** Exchange table */
    private final JTable table;

    /** Chain */
    private final Chain chain;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     * @param       chain           Chain
     * @param       responses       GetCoinExchangeOrders responses
     */
    public ExchangeDialog(JFrame parent, Chain chain, List<Response> responses) {
        super(parent, chain.getName() + " Exchange Orders", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.chain = chain;
        //
        // Create the exchange table
        //
        tableModel = new ExchangeTableModel(columnNames, columnClasses, chain, responses);
        table = new SizedTable(tableModel, columnTypes);
        table.setRowSorter(new TableRowSorter<>(tableModel));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel tablePane = new JPanel();
        tablePane.setBackground(Color.WHITE);
        tablePane.add(scrollPane);
        //
        // Create the buttons (Exchange Coins, Done)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Exchange Coins", "exchange"},
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
     * @param       chain               Chain
     * @param       responses           GetCoinExchangeOrders responses
     */
    public static void showDialog(JFrame parent, Chain chain, List<Response> responses) {
        try {
            JDialog dialog = new ExchangeDialog(parent, chain, responses);
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
        // "done"       - All done
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "done":
                    setVisible(false);
                    dispose();
                    break;
                case "exchange":
                    ExchangeCoinsDialog.showDialog(this, chain);
                    break;
            }
        } catch (Exception exc) {
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * ExchangeTableModel is the table model for the exchange dialog
     */
    private class ExchangeTableModel extends AbstractTableModel {

        /** Column names */
        private String[] columnNames;

        /** Column classes */
        private Class<?>[] columnClasses;

        /** Chain */
        private Chain chain;

        /** Open orders */
        private List<Order> orders;

        /**
         * Create the table model
         *
         * @param       columnNames     Column names
         * @param       columnClasses   Column classes
         * @param       chain           Child chain
         * @param       responses       GetCoinExchangeOrders responses
         */
        public ExchangeTableModel(String[] columnNames, Class<?>[] columnClasses,
                        Chain chain, List<Response> responses) {
            super();
            if (columnNames.length != columnClasses.length)
                throw new IllegalArgumentException("Number of names not same as number of classes");
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
            this.chain = chain;
            this.orders = new ArrayList<>(responses.size());
            responses.forEach(response -> this.orders.add(new Order(response)));
            this.orders.sort(null);
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
            return orders.size();
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
            if (row >= orders.size())
                throw new IndexOutOfBoundsException("Table row "+row+" is not valid");
            Object value;
            Order order = orders.get(row);
            switch (column) {
                case 0:                             // Exchange chain name
                    value = order.getExchangeChain().getName();
                    break;
                case 1:                             // Exchange account name
                    value = order.getExchangeAccount();
                    break;
                case 2:                             // Exchange amount
                    value = order.getExchangeAmount();
                    break;
                case 3:                             // Exchange price
                    value = order.getExchangePrice();
                    break;
                default:
                    throw new IndexOutOfBoundsException("Table column "+column+" is not valid");
            }
            return value;
        }

        /**
         * Open order
         */
        private class Order implements Comparable<Order> {

            /** Exchange chain */
            private final Chain exchangeChain;

            /** Exchange account */
            private final String account;

            /** Exchange amount */
            private final BigDecimal amount;

            /** Exchange price */
            private final BigDecimal price;

            /**
             * Create an order
             *
             * @param   response        Response to getCoinExchangeOrders
             */
            public Order(Response response) {
                exchangeChain = Nxt.getChain(response.getInt("chain"));
                amount = new BigDecimal(response.getLong("quantityQNT")).movePointLeft(chain.getDecimals())
                        .multiply(new BigDecimal(response.getLong("bidNQT")).movePointLeft(exchangeChain.getDecimals()));
                price = new BigDecimal(response.getLong("askNQT")).movePointLeft(chain.getDecimals());
                account = response.getString("accountRS");
            }

            /**
             * Get the exchange chain
             *
             * @return                  Exchange chain
             */
            public Chain getExchangeChain() {
                return exchangeChain;
            }

            /**
             * Get the exchange account
             *
             * @return                  Exchange account
             */
            public String getExchangeAccount() {
                return account;
            }

            /**
             * Get the exchange amount
             *
             * @return                  Exchange amount
             */
            public BigDecimal getExchangeAmount() {
                return amount;
            }

            /**
             * Get the exchange price
             *
             * @return                  Exchange price
             */
            public BigDecimal getExchangePrice() {
                return price;
            }

            /**
             * Compare two orders
             *
             * @param   order1          First order
             * @param   order2          Second order
             * @return                  -1 if less than, 0 if equal, 1 if greater than
             */
            @Override
            public int compareTo(Order order2) {
                int c = (exchangeChain.getName().compareTo(order2.exchangeChain.getName()));
                return (c == 0 ? price.compareTo(order2.price) : c);
            }
        }
    }
}
