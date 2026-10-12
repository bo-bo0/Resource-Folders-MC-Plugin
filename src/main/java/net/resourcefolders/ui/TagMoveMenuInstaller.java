package net.resourcefolders.ui;

import net.resourcefolders.folders.ResourceFolder;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

final class TagMoveMenuInstaller
{
    private TagMoveMenuInstaller()
    {
    }

    static void install(JTable table,
            ResourceFolderManager folderManager, Runnable refresh)
    {
        table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
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

                int row = table.rowAtPoint(event.getPoint());
                if (row < 0)
                {
                    return;
                }
                if (!table.isRowSelected(row))
                {
                    table.setRowSelectionInterval(row, row);
                }

                var popup = new JPopupMenu();
                var moveMenu = new JMenu("Move to folder");
                addTarget(moveMenu, ResourceSection.TAGS.getDisplayName(),
                        ResourceFolderData.ROOT_ID, table, folderManager, refresh);
                var children = folderManager.getChildren(ResourceSection.TAGS.getId(),
                        ResourceFolderData.ROOT_ID);
                if (!children.isEmpty())
                {
                    moveMenu.addSeparator();
                }
                for (var folder : children)
                {
                    addFolder(moveMenu, folder, table, folderManager, refresh);
                }
                popup.add(moveMenu);
                popup.show(table, event.getX(), event.getY());
            }
        });
    }

    private static void addFolder(JMenu parent, ResourceFolder folder, JTable table,
            ResourceFolderManager folderManager, Runnable refresh)
    {
        var children = folderManager.getChildren(ResourceSection.TAGS.getId(), folder.getId());
        if (children.isEmpty())
        {
            addTarget(parent, folder.getName(), folder.getId(), table,
                    folderManager, refresh);
            return;
        }

        var submenu = new JMenu(folder.getName());
        addTarget(submenu, "Move here", folder.getId(), table,
                folderManager, refresh);
        submenu.addSeparator();
        for (var child : children)
        {
            addFolder(submenu, child, table, folderManager, refresh);
        }
        parent.add(submenu);
    }

    private static void addTarget(JMenu menu, String label, String folderId,
            JTable table, ResourceFolderManager folderManager,
            Runnable refresh)
    {
        var item = new JMenuItem(label);
        item.addActionListener(_ ->
        {
            var keys = new ArrayList<String>();
            for (int row : table.getSelectedRows())
            {
                keys.add(TagTableKey.fromViewRow(table, row));
            }
            folderManager.moveResources(ResourceSection.TAGS.getId(), keys, folderId);
            refresh.run();
        });
        menu.add(item);
    }
}
