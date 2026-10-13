package net.resourcefolders.ui;

import net.resourcefolders.folders.ResourceFolder;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.function.IntFunction;

final class TableMoveMenuInstaller
{
    private TableMoveMenuInstaller()
    {
    }

    static void install(JTable table,
            ResourceFolderManager folderManager, ResourceSection section,
            IntFunction<String> rowKey, Runnable refresh)
    {
        table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                if (SwingUtilities.isRightMouseButton(event))
                {
                    FolderTableEditing.finishBeforeFolderChange(table, section);
                }
                showPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                showPopup(event);
            }

            private void showPopup(MouseEvent event)
            {
                if (!event.isPopupTrigger())
                {
                    return;
                }

                FolderTableEditing.finishBeforeFolderChange(table, section);

                int row = table.rowAtPoint(event.getPoint());
                if (row < 0)
                {
                    return;
                }
                if (!table.isRowSelected(row))
                {
                    table.changeSelection(row, 0, false, false);
                }

                var popup = new JPopupMenu();
                var moveMenu = new JMenu("Move to folder");
                addTarget(moveMenu, section.getDisplayName(),
                        ResourceFolderData.ROOT_ID, table, folderManager, section, rowKey, refresh);
                var children = folderManager.getChildren(section.getId(),
                        ResourceFolderData.ROOT_ID);
                if (!children.isEmpty())
                {
                    moveMenu.addSeparator();
                }
                for (var folder : children)
                {
                    addFolder(moveMenu, folder, table, folderManager, section, rowKey, refresh);
                }
                popup.add(moveMenu);
                popup.show(table, event.getX(), event.getY());
            }
        });
    }

    private static void addFolder(JMenu parent, ResourceFolder folder, JTable table,
            ResourceFolderManager folderManager, ResourceSection section,
            IntFunction<String> rowKey, Runnable refresh)
    {
        var children = folderManager.getChildren(section.getId(), folder.getId());
        if (children.isEmpty())
        {
            addTarget(parent, folder.getName(), folder.getId(), table,
                    folderManager, section, rowKey, refresh);
            return;
        }

        var submenu = new JMenu(folder.getName());
        addTarget(submenu, "Move here", folder.getId(), table,
                folderManager, section, rowKey, refresh);
        submenu.addSeparator();
        for (var child : children)
        {
            addFolder(submenu, child, table, folderManager, section, rowKey, refresh);
        }
        parent.add(submenu);
    }

    private static void addTarget(JMenu menu, String label, String folderId,
            JTable table, ResourceFolderManager folderManager, ResourceSection section,
            IntFunction<String> rowKey,
            Runnable refresh)
    {
        var item = new JMenuItem(label);
        item.addActionListener(_ ->
        {
            FolderTableEditing.finishBeforeFolderChange(table, section);
            var keys = new ArrayList<String>();
            for (int row : table.getSelectedRows())
            {
                keys.add(rowKey.apply(row));
            }
            folderManager.moveResources(section.getId(), keys, folderId);
            refresh.run();
        });
        menu.add(item);
    }
}
