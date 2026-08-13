package net.resourcefolders.ui;

import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.resourcefolders.folders.ResourceFolder;
import net.resourcefolders.folders.ResourceFolderData;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceFolderContentDeleter;
import net.resourcefolders.resources.ResourceSection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ResourceFolderPanel
        extends JPanel
{
    private final ModMaker mcreator;
    private final ResourceFolderManager folderManager;
    private final ResourceSection section;

    private final DefaultListModel<ResourceFolder>
            folderListModel =
            new DefaultListModel<>();

    private final JList<ResourceFolder> folderList =
            new JList<>(folderListModel);

    private final JLabel pathLabel =
            new JLabel();

    private final JButton upFolderButton =
            new JButton(
                    UIRES.get("laf.upFolder")
            );

    private final JButton renameFolderButton =
            new JButton(
                    UIRES.get("laf.renameFolder")
            );

    private final JButton deleteFolderButton =
            new JButton(
                    UIRES.get("16px.delete")
            );

    private final List<Consumer<String>>
            folderChangedListeners =
            new ArrayList<>();

    private String currentFolderId =
            ResourceFolderData.ROOT_ID;

    public ResourceFolderPanel(
            ModMaker mcreator,
            ResourceFolderManager folderManager,
            ResourceSection section)
    {
        super(new BorderLayout());

        this.mcreator = mcreator;
        this.folderManager = folderManager;
        this.section = section;

        setOpaque(false);

        initializeToolbar();
        initializeFolderList();

        refresh();
    }

    public ResourceSection getSection()
    {
        return section;
    }

    public String getCurrentFolderId()
    {
        return currentFolderId;
    }

    public void addFolderChangedListener(
            Consumer<String> listener)
    {
        folderChangedListeners.add(
                listener
        );
    }

    private void initializeToolbar()
    {
        var toolbar =
                new JToolBar();

        toolbar.setFloatable(false);
        toolbar.setOpaque(false);

        toolbar.setBorder(
                BorderFactory.createEmptyBorder(
                        3,
                        5,
                        3,
                        5
                )
        );

        var addFolderButton =
                new JButton(
                        UIRES.get("laf.newFolder")
                );

        configureToolbarButton(
                addFolderButton
        );

        configureToolbarButton(
                upFolderButton
        );

        configureToolbarButton(
                renameFolderButton
        );

        configureToolbarButton(
                deleteFolderButton
        );

        addFolderButton.setToolTipText(
                "Create resource folder"
        );

        upFolderButton.setToolTipText(
                "Go to parent folder"
        );

        renameFolderButton.setToolTipText(
                "Rename selected folder"
        );

        deleteFolderButton.setToolTipText(
                "Delete selected folder and all its contents"
        );

        addFolderButton.addActionListener(_ ->
                createFolder()
        );

        upFolderButton.addActionListener(_ ->
                goToParentFolder()
        );

        renameFolderButton.addActionListener(_ ->
                renameSelectedFolder()
        );

        deleteFolderButton.addActionListener(_ ->
                deleteSelectedFolder()
        );

        pathLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        8,
                        0,
                        8
                )
        );

        toolbar.add(addFolderButton);
        toolbar.add(upFolderButton);

        toolbar.addSeparator();

        toolbar.add(renameFolderButton);
        toolbar.add(deleteFolderButton);

        toolbar.addSeparator();

        toolbar.add(pathLabel);

        add(
                toolbar,
                BorderLayout.NORTH
        );
    }

    private void initializeFolderList()
    {
        folderList.setOpaque(false);

        folderList.setLayoutOrientation(
                JList.HORIZONTAL_WRAP
        );

        folderList.setVisibleRowCount(1);

        folderList.setFixedCellWidth(
                150
        );

        folderList.setFixedCellHeight(
                44
        );

        folderList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        folderList.setCellRenderer(
                new FolderRenderer()
        );

        folderList.addListSelectionListener(_ ->
                updateFolderActionButtons()
        );

        folderList.addMouseListener(
                new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(
                            MouseEvent event)
                    {
                        if (event.getClickCount() == 2)
                        {
                            openSelectedFolder();
                        }
                    }
                }
        );

        folderList.addKeyListener(
                new KeyAdapter()
                {
                    @Override
                    public void keyPressed(
                            KeyEvent event)
                    {
                        switch (event.getKeyCode())
                        {
                            case KeyEvent.VK_ENTER ->
                                    openSelectedFolder();

                            case KeyEvent.VK_BACK_SPACE ->
                                    goToParentFolder();

                            case KeyEvent.VK_F2 ->
                                    renameSelectedFolder();

                            case KeyEvent.VK_DELETE ->
                                    deleteSelectedFolder();
                        }
                    }
                }
        );

        var scrollPane =
                new JScrollPane(
                        folderList
                );

        scrollPane.setOpaque(false);

        scrollPane
                .getViewport()
                .setOpaque(false);

        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        );

        scrollPane.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        5,
                        4,
                        5
                )
        );

        scrollPane.setPreferredSize(
                new Dimension(
                        0,
                        56
                )
        );

        add(
                scrollPane,
                BorderLayout.CENTER
        );
    }

    private void createFolder()
    {
        var name =
                JOptionPane.showInputDialog(
                        mcreator,
                        "Folder name:",
                        "Create Resource Folder",
                        JOptionPane.PLAIN_MESSAGE
                );

        if (name == null)
        {
            return;
        }

        name = name.trim();

        if (!validateFolderName(name))
        {
            return;
        }

        if (folderManager.folderExists(
                section.getId(),
                name,
                currentFolderId))
        {
            JOptionPane.showMessageDialog(
                    mcreator,
                    "A folder with this name already exists here.",
                    "Folder Already Exists",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        var folder =
                folderManager.createFolder(
                        section.getId(),
                        name,
                        currentFolderId
                );

        refresh();

        selectFolder(
                folder.getId()
        );
    }

    private void renameSelectedFolder()
    {
        var folder =
                folderList.getSelectedValue();

        if (folder == null)
        {
            return;
        }

        var result =
                JOptionPane.showInputDialog(
                        mcreator,
                        "Folder name:",
                        "Rename Resource Folder",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        folder.getName()
                );

        if (!(result instanceof String name))
        {
            return;
        }

        name = name.trim();

        if (!validateFolderName(name))
        {
            return;
        }

        if (name.equals(folder.getName()))
        {
            return;
        }

        if (folderManager.folderExists(
                section.getId(),
                name,
                folder.getParentId(),
                folder.getId()))
        {
            JOptionPane.showMessageDialog(
                    mcreator,
                    "A folder with this name already exists here.",
                    "Folder Already Exists",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        folderManager.renameFolder(
                section.getId(),
                folder.getId(),
                name
        );

        var folderId =
                folder.getId();

        refresh();

        selectFolder(
                folderId
        );
    }

    private void deleteSelectedFolder()
    {
        var folder =
                folderList.getSelectedValue();

        if (folder == null)
        {
            return;
        }

        var folderTreeSize =
                folderManager.getFolderTreeSize(
                        section.getId(),
                        folder.getId()
                );

        var resourceKeys =
                folderManager.getResourceKeysInFolderTree(
                        section.getId(),
                        folder.getId()
                );

        int nestedFolderCount =
                Math.max(
                        0,
                        folderTreeSize - 1
                );

        var message =
                "Delete folder \""
                        + folder.getName()
                        + "\"?\n\n"
                        + "This will permanently delete:\n"
                        + resourceKeys.size()
                        + " resource(s)\n"
                        + nestedFolderCount
                        + " subfolder(s)\n\n"
                        + "Resources will also be deleted from the workspace.\n"
                        + "This action cannot be undone.";

        int result =
                JOptionPane.showConfirmDialog(
                        mcreator,
                        message,
                        "Delete Resource Folder",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (result != JOptionPane.YES_OPTION)
        {
            return;
        }

        ResourceFolderContentDeleter.deleteResources(
                mcreator,
                section,
                resourceKeys
        );

        folderManager.deleteFolderTree(
                section.getId(),
                folder.getId()
        );

        refresh();

        notifyFolderChanged();
    }

    private boolean validateFolderName(
            String name)
    {
        if (name.isEmpty())
        {
            return false;
        }

        if (!name.matches(
                "[A-Za-z0-9._ -]+"))
        {
            JOptionPane.showMessageDialog(
                    mcreator,
                    "Folder names can only contain letters, numbers, spaces, '.', '_' and '-'.",
                    "Invalid Folder Name",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;
        }

        return true;
    }

    private void openSelectedFolder()
    {
        var folder =
                folderList.getSelectedValue();

        if (folder == null)
        {
            return;
        }

        switchFolder(
                folder.getId()
        );
    }

    private void goToParentFolder()
    {
        if (ResourceFolderData.ROOT_ID.equals(
                currentFolderId))
        {
            return;
        }

        switchFolder(
                folderManager.getParentId(
                        section.getId(),
                        currentFolderId
                )
        );
    }

    private void switchFolder(
            String folderId)
    {
        currentFolderId =
                folderId;

        refresh();

        notifyFolderChanged();
    }

    private void notifyFolderChanged()
    {
        for (var listener :
                folderChangedListeners)
        {
            listener.accept(
                    currentFolderId
            );
        }
    }

    private void refresh()
    {
        folderListModel.clear();

        for (var folder :
                folderManager.getChildren(
                        section.getId(),
                        currentFolderId))
        {
            folderListModel.addElement(
                    folder
            );
        }

        pathLabel.setText(
                folderManager.getPath(
                        section.getId(),
                        section.getDisplayName(),
                        currentFolderId
                )
        );

        upFolderButton.setEnabled(
                !ResourceFolderData.ROOT_ID.equals(
                        currentFolderId
                )
        );

        updateFolderActionButtons();

        revalidate();
        repaint();
    }

    private void selectFolder(
            String folderId)
    {
        for (int i = 0;
             i < folderListModel.size();
             i++)
        {
            var folder =
                    folderListModel.get(i);

            if (folder
                    .getId()
                    .equals(folderId))
            {
                folderList.setSelectedIndex(
                        i
                );

                folderList.ensureIndexIsVisible(
                        i
                );

                return;
            }
        }
    }

    private void updateFolderActionButtons()
    {
        boolean folderSelected =
                folderList.getSelectedValue()
                        != null;

        renameFolderButton.setEnabled(
                folderSelected
        );

        deleteFolderButton.setEnabled(
                folderSelected
        );
    }

    private static void configureToolbarButton(
            JButton button)
    {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        3,
                        0,
                        3
                )
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    private static final class FolderRenderer
            extends DefaultListCellRenderer
    {
        private final Icon folderIcon =
                UIManager.getIcon(
                        "FileView.directoryIcon"
                );

        @Override
        public Component
        getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
        {
            super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
            );

            if (value
                    instanceof ResourceFolder folder)
            {
                setText(
                        folder.getName()
                );

                setIcon(
                        folderIcon
                );
            }

            setBorder(
                    BorderFactory.createEmptyBorder(
                            4,
                            8,
                            4,
                            8
                    )
            );

            return this;
        }
    }
}