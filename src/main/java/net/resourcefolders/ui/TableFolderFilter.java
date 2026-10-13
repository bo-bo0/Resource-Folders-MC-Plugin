package net.resourcefolders.ui;

import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class TableFolderFilter
{
    private final TableRowSorter<TableModel> sorter;
    private final JTable table;
    private final Supplier<String> currentFolderId;
    private final ResourceFolderManager folderManager;
    private final ResourceSection section;
    private final BiFunction<TableModel, Integer, String> modelRowKey;
    private RowFilter<? super TableModel, ? super Integer> appliedFilter;
    private RowFilter<? super TableModel, ? super Integer> searchFilter;

    TableFolderFilter(TableRowSorter<TableModel> sorter, JTable table,
            Supplier<String> currentFolderId, ResourceFolderManager folderManager,
            ResourceSection section, BiFunction<TableModel, Integer, String> modelRowKey)
    {
        this.sorter = sorter;
        this.table = table;
        this.currentFolderId = currentFolderId;
        this.folderManager = folderManager;
        this.section = section;
        this.modelRowKey = modelRowKey;
    }

    void install()
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

    void refresh()
    {
        FolderTableEditing.finishBeforeFolderChange(table, section);

        RowFilter<TableModel, Integer> folderFilter = new RowFilter<>()
        {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
            {
                var key = modelRowKey.apply(entry.getModel(), entry.getIdentifier());
                return currentFolderId.get().equals(
                        folderManager.getResourceFolder(section.getId(), key));
            }
        };

        RowFilter<? super TableModel, ? super Integer> combined = searchFilter == null
                ? folderFilter : RowFilter.andFilter(List.of(searchFilter, folderFilter));
        appliedFilter = combined;
        sorter.setRowFilter(combined);
    }
}
