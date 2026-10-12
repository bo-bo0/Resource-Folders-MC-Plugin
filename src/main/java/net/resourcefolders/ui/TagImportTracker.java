package net.resourcefolders.ui;

import net.mcreator.workspace.Workspace;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceKey;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import java.awt.event.HierarchyEvent;
import java.util.HashSet;
import java.util.Set;

final class TagImportTracker
{
    private final Workspace workspace;
    private final ResourceFolderManager folderManager;
    private final ResourceFolderPanel folderPanel;
    private final JPanel tagsPanel;
    private final Runnable refresh;
    private final Set<String> knownKeys = new HashSet<>();
    private final Timer changeTimer;

    TagImportTracker(Workspace workspace, ResourceFolderManager folderManager,
            ResourceFolderPanel folderPanel, JPanel tagsPanel, TableModel model, Runnable refresh)
    {
        this.workspace = workspace;
        this.folderManager = folderManager;
        this.folderPanel = folderPanel;
        this.tagsPanel = tagsPanel;
        this.refresh = refresh;
        resetBaseline();

        changeTimer = new Timer(75, _ -> checkForNewTags());
        changeTimer.setRepeats(false);
        model.addTableModelListener(event ->
        {
            if (event.getType() == TableModelEvent.INSERT
                    || event.getType() == TableModelEvent.DELETE
                    || event.getType() == TableModelEvent.UPDATE)
            {
                changeTimer.restart();
            }
        });
        tagsPanel.addHierarchyListener(event ->
        {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && tagsPanel.isShowing())
            {
                resetBaseline();
            }
        });
    }

    void resetBaseline()
    {
        if (changeTimer != null)
        {
            changeTimer.stop();
        }
        var current = currentKeys();
        folderManager.removeMissingResourceMappings(ResourceSection.TAGS.getId(), current);
        knownKeys.clear();
        knownKeys.addAll(current);
    }

    private void checkForNewTags()
    {
        var current = currentKeys();
        folderManager.removeMissingResourceMappings(ResourceSection.TAGS.getId(), current);
        var added = new HashSet<>(current);
        added.removeAll(knownKeys);
        knownKeys.clear();
        knownKeys.addAll(current);

        if (!tagsPanel.isShowing()
                || ResourceFolderData.ROOT_ID.equals(folderPanel.getCurrentFolderId())
                || added.isEmpty())
        {
            return;
        }

        folderManager.moveResources(ResourceSection.TAGS.getId(), added,
                folderPanel.getCurrentFolderId());
        refresh.run();
    }

    private Set<String> currentKeys()
    {
        var keys = new HashSet<String>();
        for (var tag : workspace.getTagElements().keySet())
        {
            keys.add(ResourceKey.of(tag, workspace));
        }
        return keys;
    }
}
