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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class VariableImportTracker
{
    private static final String SECTION_ID = ResourceSection.VARIABLES.getId();

    private final Workspace workspace;
    private final ResourceFolderManager folderManager;
    private final ResourceFolderPanel folderPanel;
    private final JPanel variablesPanel;
    private final TableModel model;
    private final Runnable refresh;
    private final Set<String> knownKeys = new HashSet<>();
    private final ArrayList<String> rowKeys = new ArrayList<>();
    private final Timer changeTimer;

    VariableImportTracker(Workspace workspace, ResourceFolderManager folderManager,
            ResourceFolderPanel folderPanel, JPanel variablesPanel, TableModel model,
            Runnable refresh)
    {
        this.workspace = workspace;
        this.folderManager = folderManager;
        this.folderPanel = folderPanel;
        this.variablesPanel = variablesPanel;
        this.model = model;
        this.refresh = refresh;
        rebuildRowKeys();
        replaceKnownVariables();

        changeTimer = new Timer(75, _ -> checkForNewVariables());
        changeTimer.setRepeats(false);
        model.addTableModelListener(this::tableChanged);
        variablesPanel.addHierarchyListener(event ->
        {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && variablesPanel.isShowing())
            {
                resetBaseline();
            }
        });
    }

    void resetBaseline()
    {
        changeTimer.stop();
        rebuildRowKeys();
        replaceKnownVariables();
    }

    private void tableChanged(TableModelEvent event)
    {
        if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() >= 0)
        {
            int lastRow = Math.min(event.getLastRow(), model.getRowCount() - 1);
            for (int row = event.getFirstRow(); row <= lastRow; row++)
            {
                if (row >= rowKeys.size())
                {
                    rebuildRowKeys();
                    break;
                }

                var oldKey = rowKeys.get(row);
                var newKey = VariableTableKey.fromModelRow(model, row);
                if (!oldKey.equals(newKey))
                {
                    folderManager.renameResourceKey(SECTION_ID, oldKey, newKey);
                    knownKeys.remove(oldKey);
                    knownKeys.add(newKey);
                    rowKeys.set(row, newKey);
                    SwingUtilities.invokeLater(refresh);
                }
            }
        }
        else
        {
            rebuildRowKeys();
        }
        changeTimer.restart();
    }

    private void checkForNewVariables()
    {
        var current = currentKeys();
        folderManager.removeMissingResourceMappings(SECTION_ID, current);
        var added = new HashSet<>(current);
        added.removeAll(knownKeys);
        knownKeys.clear();
        knownKeys.addAll(current);

        if (!variablesPanel.isShowing()
                || ResourceFolderData.ROOT_ID.equals(folderPanel.getCurrentFolderId())
                || added.isEmpty())
        {
            return;
        }

        folderManager.moveResources(SECTION_ID, added, folderPanel.getCurrentFolderId());
        refresh.run();
    }

    private void replaceKnownVariables()
    {
        var current = currentKeys();
        folderManager.removeMissingResourceMappings(SECTION_ID, current);
        knownKeys.clear();
        knownKeys.addAll(current);
    }

    private void rebuildRowKeys()
    {
        rowKeys.clear();
        for (int row = 0; row < model.getRowCount(); row++)
        {
            rowKeys.add(VariableTableKey.fromModelRow(model, row));
        }
    }

    private Set<String> currentKeys()
    {
        var keys = new HashSet<String>();
        for (var variable : workspace.getVariableElements())
        {
            keys.add(ResourceKey.of(variable, workspace));
        }
        return keys;
    }
}
