package net.resourcefolders.ui;

import net.mcreator.ui.init.UIRES;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;
import net.resourcefolders.ui.dnd.ResourceFolderCrumb;
import net.resourcefolders.ui.dnd.ResourceFolderTransferHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Consumer;

public final class ResourceFolderBreadcrumb
        extends JPanel
{
    private final ResourceFolderManager folderManager;
    private final ResourceSection section;

    private final Consumer<String>
            folderSelectedListener;

    private String currentFolderId =
            ResourceFolderData.ROOT_ID;

    private Runnable reloadSection;

    public ResourceFolderBreadcrumb(
            ResourceFolderManager folderManager,
            ResourceSection section,
            Consumer<String> folderSelectedListener)
    {
        super(
                new FlowLayout(
                        FlowLayout.LEFT,
                        0,
                        0
                )
        );

        this.folderManager = folderManager;
        this.section = section;
        this.folderSelectedListener =
                folderSelectedListener;

        setOpaque(false);

        setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        4,
                        0,
                        0
                )
        );
    }

    public void installAssetDropTarget(
            Runnable reloadSection)
    {
        this.reloadSection =
                reloadSection;

        reloadPath(
                currentFolderId
        );
    }

    public void reloadPath(
            String folderId)
    {
        currentFolderId =
                folderId;

        removeAll();

        var path =
                new ArrayDeque<String>();

        var visited =
                new HashSet<String>();

        var currentId =
                folderId;

        while (!ResourceFolderData.ROOT_ID.equals(
                currentId))
        {
            if (!visited.add(currentId))
            {
                break;
            }

            var folder =
                    folderManager.getFolder(
                            section.getId(),
                            currentId
                    );

            if (folder == null)
            {
                break;
            }

            path.addFirst(
                    folder.getId()
            );

            currentId =
                    folder.getParentId();

            if (currentId == null)
            {
                currentId =
                        ResourceFolderData.ROOT_ID;
            }
        }

        addCrumb(
                ResourceFolderData.ROOT_ID,
                section.getDisplayName()
        );

        for (var pathFolderId : path)
        {
            add(
                    new JLabel(
                            UIRES.get(
                                    "16px.subpath"
                            )
                    )
            );

            var folder =
                    folderManager.getFolder(
                            section.getId(),
                            pathFolderId
                    );

            if (folder != null)
            {
                addCrumb(
                        folder.getId(),
                        folder.getName()
                );
            }
        }

        revalidate();
        repaint();
    }

    private void addCrumb(
            String folderId,
            String name)
    {
        var crumb =
                new ResourceFolderCrumb(
                        folderId,
                        name
                );

        crumb.setToolTipText(
                "Open " + name
        );

        crumb.addMouseListener(
                new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(
                            MouseEvent event)
                    {
                        if (event.getButton()
                                != MouseEvent.BUTTON1)
                        {
                            return;
                        }

                        if (folderId.equals(
                                currentFolderId))
                        {
                            return;
                        }

                        folderSelectedListener.accept(
                                folderId
                        );
                    }
                }
        );

        if (reloadSection != null)
        {
            crumb.setTransferHandler(
                    new ResourceFolderTransferHandler(
                            folderManager,
                            section,
                            reloadSection
                    )
            );
        }

        add(crumb);
    }
}