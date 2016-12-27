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

import java.util.Date;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * The SizedTable class is a JTable with column sizes based on the column data types
 */
public final class SizedTable extends JTable {

    /** Date column */
    public static final int DATE = 1;

    /** Type column */
    public static final int TYPE = 2;

    /** Amount column */
    public static final int AMOUNT = 3;

    /** Status column */
    public static final int STATUS = 4;

    /** Account/transaction address */
    public static final int ADDRESS = 5;

    /** Contact name */
    public static final int NAME = 6;

    /** Uniform Resource Locator */
    public static final int URI = 7;

    /**
     * Create a new sized table
     *
     * @param       tableModel      The table model
     * @param       columnTypes     Array of column types
     */
    public SizedTable(TableModel tableModel, int[] columnTypes) {
        //
        // Create the table
        //
        super(tableModel);
        //
        // Set the cell renderers and column widths
        //
        Component component;
        TableCellRenderer renderer;
        TableColumn column;
        TableColumnModel columnModel = getColumnModel();
        TableCellRenderer headRenderer = getTableHeader().getDefaultRenderer();
        if (headRenderer instanceof DefaultTableCellRenderer) {
            DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer)headRenderer;
            defaultRenderer.setHorizontalAlignment(JLabel.CENTER);
        }
        int columnCount = tableModel.getColumnCount();
        if (columnCount > columnTypes.length)
            throw new IllegalArgumentException("More columns than column types");
        for (int i=0; i<columnCount; i++) {
            Object value = null;
            column = columnModel.getColumn(i);
            switch (columnTypes[i]) {
                case DATE:
                    column.setCellRenderer(new DateRenderer());
                    value = new Date();
                    break;
                case TYPE:
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "mmmmmmmmmmmmmmmm";                     // 16 characters
                    break;
                case AMOUNT:                                        // nnnnnn.nnnn
                    column.setCellRenderer(new AmountRenderer());
                    value = 123456789L;
                    break;
                case STATUS:                                        // 10 characters
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "mmmmmmmmmm";
                    break;
                case ADDRESS:                                       // 24 characters
                    value = "0123456789012345678901234";
                    break;
                case NAME:                                          // 32 characters
                    value = "01234567890123456789012345678901";
                    break;
                case URI:                                           // 48 characters
                    value = "012345678901234567890123456789012345678901234567";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type "+columnTypes[i]);
            }
            component = headRenderer.getTableCellRendererComponent(this, tableModel.getColumnName(i),
                                                                   false, false, 0, i);
            int headWidth = component.getPreferredSize().width;
            renderer = column.getCellRenderer();
            if (renderer == null)
                renderer = getDefaultRenderer(tableModel.getColumnClass(i));
            component = renderer.getTableCellRendererComponent(this, value, false, false, 0, i);
            int cellWidth = component.getPreferredSize().width;
            column.setPreferredWidth(Math.max(headWidth+5, cellWidth+5));
        }
        //
        // Resize all column proportionally
        //
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
    }
}
