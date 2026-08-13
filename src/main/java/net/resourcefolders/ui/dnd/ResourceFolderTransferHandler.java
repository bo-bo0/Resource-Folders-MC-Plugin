package net.resourcefolders.ui.dnd;

import net.resourcefolders.folders.ResourceFolder;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;

public final class ResourceFolderTransferHandler
        extends TransferHandler
{
    private final ResourceFolderManager folderManager;
    private final ResourceSection section;

    private final Runnable reloadSection;

    public ResourceFolderTransferHandler(
            ResourceFolderManager folderManager,
            ResourceSection section,
            Runnable reloadSection)
    {
        this.folderManager = folderManager;
        this.section = section;
        this.reloadSection = reloadSection;
    }

    @Override
    public boolean canImport(
            TransferSupport support)
    {
        if (!support.isDrop())
        {
            return false;
        }

        if (!support.isDataFlavorSupported(
                ResourceAssetTransferHandler
                        .RESOURCE_PAYLOAD_FLAVOR))
        {
            return false;
        }

        if (!(support.getComponent()
                instanceof JList<?> list))
        {
            return false;
        }

        if (!(support.getDropLocation()
                instanceof JList.DropLocation dropLocation))
        {
            return false;
        }

        if (dropLocation.isInsert())
        {
            return false;
        }

        int index =
                dropLocation.getIndex();

        if (index < 0
                || index >= list.getModel().getSize())
        {
            return false;
        }

        var target =
                list.getModel()
                        .getElementAt(index);

        if (!(target instanceof ResourceFolder))
        {
            return false;
        }

        var payload =
                getPayload(support);

        if (payload == null)
        {
            return false;
        }

        if (!section.getId().equals(
                payload.getSectionId()))
        {
            return false;
        }

        support.setDropAction(MOVE);

        return true;
    }

    @Override
    public boolean importData(
            TransferSupport support)
    {
        if (!canImport(support))
        {
            return false;
        }

        var payload =
                getPayload(support);

        if (payload == null)
        {
            return false;
        }

        var list =
                (JList<?>) support.getComponent();

        var dropLocation =
                (JList.DropLocation)
                        support.getDropLocation();

        int index =
                dropLocation.getIndex();

        var target =
                list.getModel()
                        .getElementAt(index);

        if (!(target
                instanceof ResourceFolder folder))
        {
            return false;
        }

        var resourcesToMove =
                new ArrayList<String>();

        for (var resourceKey :
                payload.getResourceKeys())
        {
            var currentFolderId =
                    folderManager.getResourceFolder(
                            section.getId(),
                            resourceKey
                    );

            if (!folder.getId().equals(
                    currentFolderId))
            {
                resourcesToMove.add(
                        resourceKey
                );
            }
        }

        if (resourcesToMove.isEmpty())
        {
            return true;
        }

        folderManager.moveResources(
                section.getId(),
                resourcesToMove,
                folder.getId()
        );

        SwingUtilities.invokeLater(
                reloadSection
        );

        return true;
    }

    private ResourceDragPayload getPayload(
            TransferSupport support)
    {
        try
        {
            var data =
                    support
                            .getTransferable()
                            .getTransferData(
                                    ResourceAssetTransferHandler
                                            .RESOURCE_PAYLOAD_FLAVOR
                            );

            if (data
                    instanceof ResourceDragPayload payload)
            {
                return payload;
            }

            return null;
        }
        catch (
                UnsupportedFlavorException
                | IOException ignored)
        {
            return null;
        }
    }
}