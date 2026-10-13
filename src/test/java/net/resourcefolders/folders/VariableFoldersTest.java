package net.resourcefolders.folders;

import com.google.gson.Gson;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.settings.WorkspaceSettings;
import net.mcreator.workspace.elements.VariableElement;
import net.mcreator.workspace.elements.VariableType;
import net.resourcefolders.resources.ResourceKey;
import net.resourcefolders.resources.ResourceSection;
import net.resourcefolders.ui.FolderTableEditing;
import net.resourcefolders.ui.VariableTableKey;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import static org.junit.jupiter.api.Assertions.*;

class VariableFoldersTest
{
    private static final String SECTION = ResourceSection.VARIABLES.getId();

    @Test
    void movingVariableChangesOnlyFolderMetadata()
    {
        var workspace = new TestWorkspace();
        var variable = new Gson().fromJson("{\"name\":\"score\",\"type\":\"number\"}",
                VariableElement.class);
        variable.setScope(VariableType.Scope.GLOBAL_SESSION);
        variable.setValue(42);
        workspace.addVariableElement(variable);

        var manager = new ResourceFolderManager(workspace);
        var folder = manager.createFolder(SECTION, "Gameplay", ResourceFolderData.ROOT_ID);
        manager.moveResources(SECTION, java.util.List.of(ResourceKey.of(variable, workspace)), folder.getId());

        assertEquals(folder.getId(), manager.getResourceFolder(SECTION, ResourceKey.of(variable, workspace)));
        assertSame(variable, workspace.getVariableElementByName("score"));
        assertEquals("number", variable.getTypeString());
        assertEquals(VariableType.Scope.GLOBAL_SESSION, variable.getScope());
        assertEquals(42, variable.getValue());
    }

    @Test
    void renameKeepsFolderAndOlderSectionsSurviveReload()
    {
        var workspace = new TestWorkspace();
        workspace.putMetadata("resource_folders", "{\"sections\":{\"textures\":{\"folders\":[],\"resourceFolders\":{}},\"tags\":{\"folders\":[],\"resourceFolders\":{}}}}");
        var manager = new ResourceFolderManager(workspace);
        var folder = manager.createFolder(SECTION, "State", ResourceFolderData.ROOT_ID);
        manager.moveResources(SECTION, java.util.List.of(ResourceKey.ofVariable("oldName")), folder.getId());
        manager.renameResourceKey(SECTION, ResourceKey.ofVariable("oldName"), ResourceKey.ofVariable("newName"));

        var reloaded = new ResourceFolderManager(workspace);
        assertEquals(folder.getId(), reloaded.getResourceFolder(SECTION, ResourceKey.ofVariable("newName")));
        assertEquals(ResourceFolderData.ROOT_ID, reloaded.getResourceFolder(SECTION, ResourceKey.ofVariable("oldName")));
        assertTrue(((String) workspace.getMetadata("resource_folders")).contains("\"textures\""));
        assertTrue(((String) workspace.getMetadata("resource_folders")).contains("\"tags\""));
    }

    @Test
    void renamingRootVariableDoesNotInheritAStaleFolder()
    {
        var workspace = new TestWorkspace();
        var manager = new ResourceFolderManager(workspace);
        var folder = manager.createFolder(SECTION, "Old", ResourceFolderData.ROOT_ID);
        manager.moveResources(SECTION, java.util.List.of(ResourceKey.ofVariable("reusedName")), folder.getId());

        manager.renameResourceKey(SECTION, ResourceKey.ofVariable("rootVariable"),
                ResourceKey.ofVariable("reusedName"));

        assertEquals(ResourceFolderData.ROOT_ID,
                manager.getResourceFolder(SECTION, ResourceKey.ofVariable("reusedName")));
    }

    @Test
    void folderChangeCommitsVariableCellEditing() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            var table = new JTable(new DefaultTableModel(new Object[][]{{"score"}}, new Object[]{"Name"}));
            assertTrue(table.editCellAt(0, 0));
            ((JTextField) table.getEditorComponent()).setText("newScore");

            FolderTableEditing.finishBeforeFolderChange(table, ResourceSection.VARIABLES);

            assertFalse(table.isEditing());
            assertEquals("newScore", table.getModel().getValueAt(0, 0));
        });
    }

    @Test
    void variableKeyUsesModelNameAfterSortingAndColumnReordering() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            var model = new DefaultTableModel(
                    new Object[][]{{"zeta", "number"}, {"alpha", "logic"}},
                    new Object[]{"Name", "Type"});
            var table = new JTable(model);
            var sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.toggleSortOrder(0);
            table.moveColumn(0, 1);

            assertEquals(ResourceKey.ofVariable("alpha"), VariableTableKey.fromViewRow(table, 0));
            assertEquals(ResourceKey.ofVariable("zeta"), VariableTableKey.fromViewRow(table, 1));
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
