package net.resourcefolders.resources;

import net.mcreator.workspace.Workspace;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.ui.ResourceFolderPanel;

import javax.swing.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ResourceImportTracker
{
    private static final int CHECK_INTERVAL_MS = 300;

    private final Workspace workspace;
    private final ResourceFolderManager folderManager;
    private final ResourceSection section;
    private final ResourceFolderPanel folderPanel;

    private final Supplier<? extends Collection<?>> resourcesSupplier;
    private final BooleanSupplier sectionActiveSupplier;
    private final Runnable refresh;

    private final Set<String> knownResourceKeys =
            new HashSet<>();

    private final Timer timer;

    private boolean sectionWasActive;
    private boolean becameDisplayable;

    public ResourceImportTracker(
            Workspace workspace,
            ResourceFolderManager folderManager,
            ResourceSection section,
            ResourceFolderPanel folderPanel,
            Supplier<? extends Collection<?>> resourcesSupplier,
            BooleanSupplier sectionActiveSupplier,
            Runnable refresh)
    {
        this.workspace = workspace;
        this.folderManager = folderManager;
        this.section = section;
        this.folderPanel = folderPanel;
        this.resourcesSupplier = resourcesSupplier;
        this.sectionActiveSupplier = sectionActiveSupplier;
        this.refresh = refresh;

        replaceKnownResources();

        sectionWasActive =
                sectionActiveSupplier.getAsBoolean();

        timer = new Timer(
                CHECK_INTERVAL_MS,
                _ -> checkForNewResources()
        );

        timer.setCoalesce(true);
        timer.start();
    }

    public void resetBaseline()
    {
        replaceKnownResources();
    }

    private void checkForNewResources()
    {
        updateLifecycleState();

        if (!timer.isRunning())
        {
            return;
        }

        boolean sectionActive =
                sectionActiveSupplier.getAsBoolean();

        if (sectionActive && !sectionWasActive)
        {
            replaceKnownResources();

            sectionWasActive = true;

            return;
        }

        if (!sectionActive)
        {
            sectionWasActive = false;

            return;
        }

        var currentFolderId =
                folderPanel.getCurrentFolderId();

        if (ResourceFolderData.ROOT_ID.equals(
                currentFolderId))
        {
            return;
        }

        var currentResourceKeys =
                getCurrentResourceKeys();

        var newResourceKeys =
                new HashSet<>(currentResourceKeys);

        newResourceKeys.removeAll(
                knownResourceKeys
        );

        knownResourceKeys.clear();
        knownResourceKeys.addAll(
                currentResourceKeys
        );

        if (newResourceKeys.isEmpty())
        {
            return;
        }

        folderManager.moveResources(
                section.getId(),
                newResourceKeys,
                currentFolderId
        );

        refresh.run();
    }

    private void replaceKnownResources()
    {
        knownResourceKeys.clear();

        knownResourceKeys.addAll(
                getCurrentResourceKeys()
        );
    }

    private Set<String> getCurrentResourceKeys()
    {
        var resourceKeys =
                new HashSet<String>();

        for (var resource : resourcesSupplier.get())
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

        return resourceKeys;
    }

    private void updateLifecycleState()
    {
        if (folderPanel.isDisplayable())
        {
            becameDisplayable = true;

            return;
        }

        if (becameDisplayable)
        {
            timer.stop();
        }
    }
}