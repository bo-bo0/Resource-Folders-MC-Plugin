package net.resourcefolders.ui;

import net.resourcefolders.resources.ResourceKey;

import javax.swing.*;
import javax.swing.table.TableModel;

public final class VariableTableKey
{
    private VariableTableKey()
    {
    }

    public static String fromViewRow(JTable table, int row)
    {
        return fromModelRow(table.getModel(), table.convertRowIndexToModel(row));
    }

    public static String fromModelRow(TableModel model, int row)
    {
        return ResourceKey.ofVariable(model.getValueAt(row, 0).toString());
    }
}
