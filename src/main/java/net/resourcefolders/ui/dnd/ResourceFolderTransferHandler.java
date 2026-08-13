package net.resourcefolders.ui.dnd;

import net.resourcefolders.folders.ResourceFolder;
import net.resourcefolders.folders.ResourceFolderData;
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
        boolean accepted =
                canImportInternal(support);

        if (support.getComponent()
                instanceof ResourceFolderCrumb crumb)
        {
            if (accepted)
            {
                crumb.pulseDropHighlight();
            }
            else
            {
                crumb.clearDropHighlight();
            }
        }

        if (accepted)
        {
            support.setDropAction(MOVE);
        }

        return accepted;
    }

    @Override
    public boolean importData(
            TransferSupport support)
    {
        if (!canImportInternal(support))
        {
            clearCrumbHighlight(
                    support
            );

            return false;
        }

        var payload =
                getPayload(support);

        if (payload == null)
        {
            clearCrumbHighlight(
                    support
            );

            return false;
        }

        var targetFolderId =
                getTargetFolderId(
                        support
                );

        if (targetFolderId == null)
        {
            clearCrumbHighlight(
                    support
            );

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

            if (!targetFolderId.equals(
                    currentFolderId))
            {
                resourcesToMove.add(
                        resourceKey
                );
            }
        }

        clearCrumbHighlight(
                support
        );

        if (resourcesToMove.isEmpty())
        {
            return true;
        }

        folderManager.moveResources(
                section.getId(),
                resourcesToMove,
                targetFolderId
        );

        SwingUtilities.invokeLater(
                reloadSection
        );

        return true;
    }

    private boolean canImportInternal(
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

        var targetFolderId =
                getTargetFolderId(
                        support
                );

        if (targetFolderId == null)
        {
            return false;
        }

        if (ResourceFolderData.ROOT_ID.equals(
                targetFolderId))
        {
            return true;
        }

        return folderManager.getFolder(
                section.getId(),
                targetFolderId
        ) != null;
    }

    private String getTargetFolderId(
            TransferSupport support)
    {
        var component =
                support.getComponent();

        if (component
                instanceof ResourceFolderCrumb crumb)
        {
            return crumb.getFolderId();
        }

        if (component
                instanceof JList<?> list)
        {
            if (!(support.getDropLocation()
                    instanceof JList.DropLocation
                    dropLocation))
            {
                return null;
            }

            if (dropLocation.isInsert())
            {
                return null;
            }

            int index =
                    dropLocation.getIndex();

            if (index < 0
                    || index
                    >= list.getModel().getSize())
            {
                return null;
            }

            var target =
                    list.getModel()
                            .getElementAt(index);

            if (target
                    instanceof ResourceFolder folder)
            {
                return folder.getId();
            }
        }

        return null;
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

    private void clearCrumbHighlight(
            TransferSupport support)
    {
        if (support.getComponent()
                instanceof ResourceFolderCrumb crumb)
        {
            crumb.clearDropHighlight();
        }
    }
}