package net.resourcefolders.ui;

import javax.swing.*;

public final class TagTableEditing
{
    private TagTableEditing()
    {
    }

    public static void cancelBeforeFolderChange(JTable table)
    {
        if (table.isEditing())
        {
            table.getCellEditor().cancelCellEditing();
        }
    }
}
