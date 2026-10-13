package net.resourcefolders.ui;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.ui.workspace.AbstractWorkspacePanel;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;
import net.resourcefolders.ui.dnd.ResourceAssetTransferHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public final class VariableFoldersInstaller
{
    private static final Logger LOG = LogManager.getLogger("Resource Folders plugin");
    private static final String INSTALLED_PROPERTY = "resourceFolders.variablesInstalled";

    private VariableFoldersInstaller()
    {
    }

    @SuppressWarnings("unchecked")
    public static void install(ModMaker mcreator, ResourceFolderManager folderManager)
    {
        var variablesPanel = findVariablesPanel(mcreator.getWorkspacePanel());
        if (variablesPanel == null || !variablesPanel.isSupportedInWorkspace()
                || Boolean.TRUE.equals(variablesPanel.getClientProperty(INSTALLED_PROPERTY)))
        {
            return;
        }

        var table = findTable(variablesPanel);
        if (table == null || !(table.getRowSorter() instanceof TableRowSorter<?>)
                || !(variablesPanel.getLayout() instanceof BorderLayout layout))
        {
            LOG.warn("Resource Folders: Variables table or sorter was not found");
            return;
        }

        var sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        var folderPanel = new ResourceFolderPanel(mcreator, folderManager, ResourceSection.VARIABLES);
        folderPanel.setBeforeContentsChange(() ->
                FolderTableEditing.finishBeforeFolderChange(table, ResourceSection.VARIABLES));
        var filter = new TableFolderFilter(sorter, table, folderPanel::getCurrentFolderId, folderManager,
                ResourceSection.VARIABLES, VariableTableKey::fromModelRow);
        filter.install();

        var originalNorth = layout.getLayoutComponent(BorderLayout.NORTH);
        if (originalNorth != null)
        {
            variablesPanel.remove(originalNorth);
        }
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        if (originalNorth != null)
        {
            header.add(originalNorth, BorderLayout.NORTH);
        }
        header.add(folderPanel, BorderLayout.CENTER);
        variablesPanel.add(header, BorderLayout.NORTH);

        folderPanel.installAssetDropTarget(filter::refresh);
        table.setTransferHandler(new ResourceAssetTransferHandler(
                mcreator.getWorkspace(), ResourceSection.VARIABLES));
        table.setDragEnabled(true);
        TableMoveMenuInstaller.install(table, folderManager, ResourceSection.VARIABLES,
                row -> VariableTableKey.fromViewRow(table, row), filter::refresh);

        var tracker = new VariableImportTracker(mcreator.getWorkspace(), folderManager,
                folderPanel, variablesPanel, table.getModel(), filter::refresh);
        folderPanel.addFolderChangedListener(_ -> filter.refresh());
        folderPanel.addResourcesChangedListener(() ->
        {
            FolderTableEditing.finishBeforeFolderChange(table, ResourceSection.VARIABLES);
            variablesPanel.reloadElements();
            tracker.resetBaseline();
            filter.refresh();
        });

        variablesPanel.putClientProperty(INSTALLED_PROPERTY, Boolean.TRUE);
        variablesPanel.revalidate();
        variablesPanel.repaint();
        LOG.info("Resource Folders: installed Variables section");
    }

    private static AbstractWorkspacePanel findVariablesPanel(Container container)
    {
        for (var component : container.getComponents())
        {
            if (component instanceof AbstractWorkspacePanel panel
                    && "WorkspacePanelVariables".equals(panel.getClass().getSimpleName()))
            {
                return panel;
            }
            if (component instanceof Container child)
            {
                var found = findVariablesPanel(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private static JTable findTable(Container container)
    {
        for (var component : container.getComponents())
        {
            if (component instanceof JTable table)
            {
                return table;
            }
            if (component instanceof Container child)
            {
                var found = findTable(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }
}
