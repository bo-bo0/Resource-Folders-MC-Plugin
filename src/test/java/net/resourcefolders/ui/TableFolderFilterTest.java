package net.resourcefolders.ui;

import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.settings.WorkspaceSettings;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceKey;
import net.resourcefolders.resources.ResourceSection;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableFolderFilterTest
{
    @Test
    void folderFilterKeepsNativeSearchFilter() throws Exception
    {
        var manager = new ResourceFolderManager(new TestWorkspace());
        var section = ResourceSection.VARIABLES;
        var folder = manager.createFolder(section.getId(), "Named", ResourceFolderData.ROOT_ID);
        manager.moveResources(section.getId(), List.of(ResourceKey.ofVariable("alpha")), folder.getId());

        SwingUtilities.invokeAndWait(() ->
        {
            TableModel model = new DefaultTableModel(new Object[][]{{"alpha"}, {"beta"}},
                    new Object[]{"Name"});
            var table = new JTable(model);
            var sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.setRowFilter(RowFilter.regexFilter("^a"));
            var currentFolder = new AtomicReference<>(ResourceFolderData.ROOT_ID);
            var filter = new TableFolderFilter(sorter, table, currentFolder::get, manager,
                    section, VariableTableKey::fromModelRow);
            filter.install();

            assertEquals(0, table.getRowCount());
            currentFolder.set(folder.getId());
            filter.refresh();
            assertEquals(1, table.getRowCount());
            assertEquals("alpha", table.getValueAt(0, 0));

            sorter.setRowFilter(RowFilter.regexFilter("^b"));
            assertEquals(0, table.getRowCount());
            currentFolder.set(ResourceFolderData.ROOT_ID);
            filter.refresh();
            assertEquals(1, table.getRowCount());
            assertEquals("beta", table.getValueAt(0, 0));
        });
    }

    private static final class TestWorkspace extends Workspace
    {
        private TestWorkspace()
        {
            super((WorkspaceSettings) null);
        }
    }
}
