package net.resourcefolders.ui;

import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;

public final class FolderTableEditing
{
    private FolderTableEditing()
    {
    }

    public static void cancelBeforeFolderChange(JTable table)
    {
        if (table.isEditing())
        {
            table.getCellEditor().cancelCellEditing();
        }
    }

    public static void finishBeforeFolderChange(JTable table, ResourceSection section)
    {
        if (!table.isEditing())
        {
            return;
        }

        if (section == ResourceSection.VARIABLES)
        {
            var editor = table.getCellEditor();
            if (!editor.stopCellEditing())
            {
                editor.cancelCellEditing();
            }
        }
        else
        {
            cancelBeforeFolderChange(table);
        }
    }
}
