package net.resourcefolders.resources;

import net.mcreator.workspace.Workspace;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.ui.ResourceFolderPanel;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ResourceImportTracker
{
    private static final int CHANGE_DEBOUNCE_MS = 50;

    private final Workspace workspace;
    private final ResourceFolderManager folderManager;
    private final ResourceSection section;
    private final ResourceFolderPanel folderPanel;

    private final Set<ListModel<?>> resourceModels;

    private final Supplier<? extends Collection<?>>
            resourcesSupplier;

    private final BooleanSupplier
            sectionActiveSupplier;

    private final Runnable refresh;

    private final Set<String> knownResourceKeys =
            new HashSet<>();

    private final Timer changeDebounceTimer;

    private final ListDataListener resourceChangesListener =
            new ListDataListener()
            {
                @Override
                public void intervalAdded(
                        ListDataEvent event)
                {
                    scheduleCheck();
                }

                @Override
                public void intervalRemoved(
                        ListDataEvent event)
                {
                    scheduleCheck();
                }

                @Override
                public void contentsChanged(
                        ListDataEvent event)
                {
                    scheduleCheck();
                }
            };

    private final HierarchyListener hierarchyListener =
            this::hierarchyChanged;

    private boolean sectionWasActive;
    private boolean becameDisplayable;
    private boolean disposed;

    public ResourceImportTracker(
            Workspace workspace,
            ResourceFolderManager folderManager,
            ResourceSection section,
            ResourceFolderPanel folderPanel,
            Collection<? extends ListModel<?>> resourceModels,
            Supplier<? extends Collection<?>> resourcesSupplier,
            BooleanSupplier sectionActiveSupplier,
            Runnable refresh)
    {
        this.workspace = workspace;
        this.folderManager = folderManager;
        this.section = section;
        this.folderPanel = folderPanel;
        this.resourceModels =
                new LinkedHashSet<>(
                        resourceModels
                );
        this.resourcesSupplier = resourcesSupplier;
        this.sectionActiveSupplier = sectionActiveSupplier;
        this.refresh = refresh;

        replaceKnownResources();

        sectionWasActive =
                sectionActiveSupplier.getAsBoolean();

        becameDisplayable =
                folderPanel.isDisplayable();

        changeDebounceTimer =
                new Timer(
                        CHANGE_DEBOUNCE_MS,
                        _ ->
                                checkForNewResources()
                );

        changeDebounceTimer.setCoalesce(true);
        changeDebounceTimer.setRepeats(false);

        this.resourceModels.forEach(
                model ->
                        model.addListDataListener(
                                resourceChangesListener
                        )
        );

        folderPanel.addHierarchyListener(
                hierarchyListener
        );
    }

    public void resetBaseline()
    {
        if (disposed)
        {
            return;
        }

        changeDebounceTimer.stop();

        replaceKnownResources();
    }

    public void sectionActivityChanged()
    {
        updateSectionActivity();
    }

    private boolean updateSectionActivity()
    {
        if (disposed)
        {
            return false;
        }

        boolean sectionActive =
                sectionActiveSupplier
                        .getAsBoolean();

        boolean becameActive =
                sectionActive
                        && !sectionWasActive;

        if (becameActive)
        {
            changeDebounceTimer.stop();

            replaceKnownResources();
        }

        sectionWasActive = sectionActive;

        return becameActive;
    }

    private void scheduleCheck()
    {
        if (!SwingUtilities
                .isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    this::scheduleCheck
            );

            return;
        }

        if (updateSectionActivity())
        {
            return;
        }

        if (!sectionWasActive)
        {
            return;
        }

        if (ResourceFolderData.ROOT_ID.equals(
                folderPanel.getCurrentFolderId()))
        {
            return;
        }

        changeDebounceTimer.restart();
    }

    private void checkForNewResources()
    {
        if (updateSectionActivity())
        {
            return;
        }

        if (!sectionWasActive)
        {
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
                new HashSet<>(
                        currentResourceKeys
                );

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

        for (var resource :
                resourcesSupplier.get())
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

    private void hierarchyChanged(
            HierarchyEvent event)
    {
        if ((event.getChangeFlags()
                & HierarchyEvent.DISPLAYABILITY_CHANGED)
                != 0)
        {
            if (folderPanel.isDisplayable())
            {
                becameDisplayable = true;
            }
            else if (becameDisplayable)
            {
                dispose();

                return;
            }
        }

        if ((event.getChangeFlags()
                & HierarchyEvent.SHOWING_CHANGED)
                != 0)
        {
            sectionActivityChanged();
        }
    }

    private void dispose()
    {
        if (disposed)
        {
            return;
        }

        disposed = true;

        changeDebounceTimer.stop();

        resourceModels.forEach(
                model ->
                        model.removeListDataListener(
                                resourceChangesListener
                        )
        );

        folderPanel.removeHierarchyListener(
                hierarchyListener
        );
    }
}
