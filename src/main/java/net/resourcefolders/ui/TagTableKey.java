package net.resourcefolders.ui;

import net.mcreator.minecraft.TagType;
import net.mcreator.workspace.elements.TagElement;
import net.resourcefolders.resources.ResourceKey;

import javax.swing.*;
import javax.swing.table.TableModel;

public final class TagTableKey
{
    private TagTableKey()
    {
    }

    public static String fromViewRow(JTable table, int row)
    {
        return of((TagType) table.getValueAt(row, 0),
                table.getValueAt(row, 1).toString(), table.getValueAt(row, 2).toString());
    }

    public static String fromModelRow(TableModel model, int row)
    {
        return of((TagType) model.getValueAt(row, 0),
                model.getValueAt(row, 1).toString(), model.getValueAt(row, 2).toString());
    }

    private static String of(TagType type, String namespace, String name)
    {
        return ResourceKey.ofTag(new TagElement(type, namespace + ":" + name));
    }
}
