package net.resourcefolders.ui;

import net.mcreator.workspace.Workspace;
import net.resourcefolders.folders.ResourceFolder;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceKey;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public final class ResourceMoveMenuInstaller
{
    private ResourceMoveMenuInstaller()
    {
    }

    public static void install(
            JList<?> list,
            Workspace workspace,
            ResourceFolderManager folderManager,
            ResourceSection section,
            Runnable refresh)
    {
        list.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                showPopupIfNeeded(event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                showPopupIfNeeded(event);
            }

            private void showPopupIfNeeded(MouseEvent event)
            {
                if (!event.isPopupTrigger())
                {
                    return;
                }

                var point = event.getPoint();
                int index = list.locationToIndex(point);

                if (index < 0)
                {
                    return;
                }

                var bounds = list.getCellBounds(index, index);

                if (bounds == null || !bounds.contains(point))
                {
                    return;
                }

                if (!list.isSelectedIndex(index))
                {
                    list.setSelectedIndex(index);
                }

                var popupMenu = createPopupMenu(
                        list,
                        workspace,
                        folderManager,
                        section,
                        refresh
                );

                popupMenu.show(
                        list,
                        event.getX(),
                        event.getY()
                );
            }
        });
    }

    private static JPopupMenu createPopupMenu(
            JList<?> list,
            Workspace workspace,
            ResourceFolderManager folderManager,
            ResourceSection section,
            Runnable refresh)
    {
        var popupMenu = new JPopupMenu();

        var moveMenu =
                new JMenu("Move to folder");

        var rootItem =
                new JMenuItem(section.getDisplayName());

        rootItem.addActionListener(_ ->
                moveSelectedResources(
                        list,
                        workspace,
                        folderManager,
                        section,
                        ResourceFolderData.ROOT_ID,
                        refresh
                )
        );

        moveMenu.add(rootItem);

        var rootFolders =
                folderManager.getChildren(
                        section.getId(),
                        ResourceFolderData.ROOT_ID
                );

        if (!rootFolders.isEmpty())
        {
            moveMenu.addSeparator();
        }

        for (var folder : rootFolders)
        {
            addFolderEntry(
                    moveMenu,
                    folder,
                    list,
                    workspace,
                    folderManager,
                    section,
                    refresh
            );
        }

        popupMenu.add(moveMenu);

        return popupMenu;
    }

    private static void addFolderEntry(
            JMenu parentMenu,
            ResourceFolder folder,
            JList<?> list,
            Workspace workspace,
            ResourceFolderManager folderManager,
            ResourceSection section,
            Runnable refresh)
    {
        var children =
                folderManager.getChildren(
                        section.getId(),
                        folder.getId()
                );

        if (children.isEmpty())
        {
            var folderItem =
                    new JMenuItem(folder.getName());

            folderItem.addActionListener(_ ->
                    moveSelectedResources(
                            list,
                            workspace,
                            folderManager,
                            section,
                            folder.getId(),
                            refresh
                    )
            );

            parentMenu.add(folderItem);

            return;
        }

        var folderMenu =
                new JMenu(folder.getName());

        var moveHereItem =
                new JMenuItem("Move here");

        moveHereItem.addActionListener(_ ->
                moveSelectedResources(
                        list,
                        workspace,
                        folderManager,
                        section,
                        folder.getId(),
                        refresh
                )
        );

        folderMenu.add(moveHereItem);
        folderMenu.addSeparator();

        for (var child : children)
        {
            addFolderEntry(
                    folderMenu,
                    child,
                    list,
                    workspace,
                    folderManager,
                    section,
                    refresh
            );
        }

        parentMenu.add(folderMenu);
    }

    private static void moveSelectedResources(
            JList<?> list,
            Workspace workspace,
            ResourceFolderManager folderManager,
            ResourceSection section,
            String targetFolderId,
            Runnable refresh)
    {
        var resourceKeys =
                new ArrayList<String>();

        for (var resource :
                list.getSelectedValuesList())
        {
            resourceKeys.add(
                    ResourceKey.of(
                            resource,
                            workspace
                    )
            );
        }

        folderManager.moveResources(
                section.getId(),
                resourceKeys,
                targetFolderId
        );

        list.clearSelection();

        refresh.run();
    }
}