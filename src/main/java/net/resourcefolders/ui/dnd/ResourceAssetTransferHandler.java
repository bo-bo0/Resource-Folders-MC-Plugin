package net.resourcefolders.ui.dnd;

import net.mcreator.workspace.Workspace;
import net.resourcefolders.resources.ResourceKey;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;

public final class ResourceAssetTransferHandler
        extends TransferHandler
{
    public static final DataFlavor RESOURCE_PAYLOAD_FLAVOR =
            new DataFlavor(
                    ResourceDragPayload.class,
                    "Resource Folder Asset Payload"
            );

    private final Workspace workspace;
    private final ResourceSection section;

    public ResourceAssetTransferHandler(
            Workspace workspace,
            ResourceSection section)
    {
        this.workspace = workspace;
        this.section = section;
    }

    @Override
    protected Transferable createTransferable(
            JComponent component)
    {
        if (!(component instanceof JList<?> list))
        {
            return null;
        }

        var resourceKeys =
                new ArrayList<String>();

        for (var resource :
                list.getSelectedValuesList())
        {
            try
            {
                resourceKeys.add(
                        ResourceKey.of(
                                resource,
                                workspace
                        )
                );
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }

        if (resourceKeys.isEmpty())
        {
            return null;
        }

        var payload =
                new ResourceDragPayload(
                        section.getId(),
                        resourceKeys
                );

        return new ResourcePayloadTransferable(
                payload
        );
    }

    @Override
    public int getSourceActions(
            JComponent component)
    {
        return MOVE;
    }

    private static final class
    ResourcePayloadTransferable
            implements Transferable
    {
        private final ResourceDragPayload payload;

        private ResourcePayloadTransferable(
                ResourceDragPayload payload)
        {
            this.payload = payload;
        }

        @Override
        public DataFlavor[]
        getTransferDataFlavors()
        {
            return new DataFlavor[]
                    {
                            RESOURCE_PAYLOAD_FLAVOR
                    };
        }

        @Override
        public boolean isDataFlavorSupported(
                DataFlavor flavor)
        {
            return RESOURCE_PAYLOAD_FLAVOR
                    .equals(flavor);
        }

        @Override
        public Object getTransferData(
                DataFlavor flavor)
                throws UnsupportedFlavorException
        {
            if (!isDataFlavorSupported(flavor))
            {
                throw new UnsupportedFlavorException(
                        flavor
                );
            }

            return payload;
        }
    }
}