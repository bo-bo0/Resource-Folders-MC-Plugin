package net.resourcefolders.ui;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.ui.workspace.WorkspacePanelTags;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;
import net.resourcefolders.ui.dnd.ResourceAssetTransferHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public final class TagFoldersInstaller
{
    private static final Logger LOG = LogManager.getLogger("Resource Folders plugin");
    private static final String INSTALLED_PROPERTY = "resourceFolders.tagsInstalled";

    private TagFoldersInstaller()
    {
    }

    @SuppressWarnings("unchecked")
    public static void install(ModMaker mcreator, ResourceFolderManager folderManager)
    {
        WorkspacePanelTags tagsPanel = mcreator.getWorkspacePanel()
                .getVerticalTab(WorkspacePanelTags.class);

        if (tagsPanel == null || !tagsPanel.isSupportedInWorkspace()
                || Boolean.TRUE.equals(tagsPanel.getClientProperty(INSTALLED_PROPERTY)))
        {
            return;
        }

        var table = findTable(tagsPanel);
        if (table == null || !(table.getRowSorter() instanceof TableRowSorter<?>)
                || !(tagsPanel.getLayout() instanceof BorderLayout))
        {
            LOG.warn("Resource Folders: Element Tags table or sorter was not found");
            return;
        }

        var sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        var folderPanel = new ResourceFolderPanel(mcreator, folderManager, ResourceSection.TAGS);
        var filter = new TagFolderFilter(sorter, table, folderPanel, folderManager);
        filter.install();

        var originalNorth = ((BorderLayout) tagsPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (originalNorth != null)
        {
            tagsPanel.remove(originalNorth);
        }

        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        if (originalNorth != null)
        {
            header.add(originalNorth, BorderLayout.NORTH);
        }
        header.add(folderPanel, BorderLayout.CENTER);
        tagsPanel.add(header, BorderLayout.NORTH);

        folderPanel.installAssetDropTarget(filter::refresh);
        table.setTransferHandler(new ResourceAssetTransferHandler(
                mcreator.getWorkspace(), ResourceSection.TAGS));
        table.setDragEnabled(true);
        TagMoveMenuInstaller.install(table, folderManager, filter::refresh);

        var tracker = new TagImportTracker(mcreator.getWorkspace(), folderManager,
                folderPanel, tagsPanel, table.getModel(), filter::refresh);
        folderPanel.addFolderChangedListener(_ -> filter.refresh());
        folderPanel.addResourcesChangedListener(() ->
        {
            TagTableEditing.cancelBeforeFolderChange(table);
            tagsPanel.reloadElements();
            tracker.resetBaseline();
            filter.refresh();
        });

        tagsPanel.putClientProperty(INSTALLED_PROPERTY, Boolean.TRUE);
        tagsPanel.revalidate();
        tagsPanel.repaint();
        LOG.info("Resource Folders: installed Element Tags section");
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
                var table = findTable(child);
                if (table != null)
                {
                    return table;
                }
            }
        }
        return null;
    }

    private static final class TagFolderFilter
    {
        private final TableRowSorter<TableModel> sorter;
        private final JTable table;
        private final ResourceFolderPanel folderPanel;
        private final ResourceFolderManager folderManager;
        private RowFilter<? super TableModel, ? super Integer> appliedFilter;
        private RowFilter<? super TableModel, ? super Integer> searchFilter;

        private TagFolderFilter(TableRowSorter<TableModel> sorter, JTable table,
                ResourceFolderPanel folderPanel, ResourceFolderManager folderManager)
        {
            this.sorter = sorter;
            this.table = table;
            this.folderPanel = folderPanel;
            this.folderManager = folderManager;
        }

        private void install()
        {
            searchFilter = sorter.getRowFilter();
            sorter.addRowSorterListener(event ->
            {
                if (event.getType() == RowSorterEvent.Type.SORTED
                        && sorter.getRowFilter() != appliedFilter)
                {
                    searchFilter = sorter.getRowFilter();
                    refresh();
                }
            });
            refresh();
        }

        private void refresh()
        {
            TagTableEditing.cancelBeforeFolderChange(table);

            RowFilter<TableModel, Integer> folderFilter = new RowFilter<>()
            {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
                {
                    var key = TagTableKey.fromModelRow(entry.getModel(), entry.getIdentifier());
                    return folderPanel.getCurrentFolderId().equals(
                            folderManager.getResourceFolder(ResourceSection.TAGS.getId(), key));
                }
            };

            RowFilter<? super TableModel, ? super Integer> combined = searchFilter == null
                    ? folderFilter : RowFilter.andFilter(List.of(searchFilter, folderFilter));
            appliedFilter = combined;
            sorter.setRowFilter(combined);
        }
    }
}
