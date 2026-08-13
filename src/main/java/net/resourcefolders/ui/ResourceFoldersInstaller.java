package net.resourcefolders.ui;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceKey;
import net.resourcefolders.resources.ResourceSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import net.mcreator.ui.workspace.IReloadableFilterable;

import java.io.File;
import java.util.List;

import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.resources.Animation;
import net.mcreator.workspace.resources.Model;
import net.resourcefolders.resources.ResourceImportTracker;

import java.util.Collection;

public final class ResourceFoldersInstaller
{
    private static final Logger LOG =
            LogManager.getLogger("Resource Folders plugin");

    private static final String INSTALLED_PROPERTY =
            "resourceFolders.installed";

    private ResourceFoldersInstaller()
    {
    }

    public static void install(ModMaker mcreator)
    {
        var workspacePanel =
                mcreator.getWorkspacePanel();

        var resourcesPanel =
                workspacePanel.resourcesPan;

        if (Boolean.TRUE.equals(
                resourcesPanel.getClientProperty(
                        INSTALLED_PROPERTY)))
        {
            return;
        }

        var resourceTabs =
                findResourceTabs(resourcesPanel);

        if (resourceTabs == null)
        {
            LOG.error(
                    "Resource Folders: could not find Resources JTabbedPane"
            );

            return;
        }

        var folderManager =
                new ResourceFolderManager(
                        mcreator.getWorkspace()
                );

        int installedSections = 0;

        for (int i = 0;
             i < resourceTabs.getTabCount();
             i++)
        {
            var component =
                    resourceTabs.getComponentAt(i);

            if (!(component instanceof JPanel panel))
            {
                continue;
            }

            var section =
                    ResourceSection.fromPanel(panel);

            if (section == null)
            {
                continue;
            }

            installIntoResourcePanel(
                    mcreator,
                    resourceTabs,
                    panel,
                    section,
                    folderManager
            );

            installedSections++;
        }

        resourcesPanel.putClientProperty(
                INSTALLED_PROPERTY,
                Boolean.TRUE
        );

        LOG.info(
                "Resource Folders: installed into {} resource sections",
                installedSections
        );

        resourcesPanel.revalidate();
        resourcesPanel.repaint();
    }

    private static void installIntoResourcePanel(
            ModMaker mcreator,
            JTabbedPane resourceTabs,
            JPanel resourcePanel,
            ResourceSection section,
            ResourceFolderManager folderManager)
    {
        if (!(resourcePanel.getLayout()
                instanceof BorderLayout layout))
        {
            LOG.warn(
                    "Resource Folders: {} does not use BorderLayout",
                    resourcePanel.getClass().getName()
            );

            return;
        }

        var resourceLists =
                findResourceLists(resourcePanel);

        var originalNorth =
                layout.getLayoutComponent(
                        BorderLayout.NORTH
                );

        if (originalNorth != null)
        {
            resourcePanel.remove(originalNorth);
        }

        var folderPanel =
                new ResourceFolderPanel(
                        mcreator,
                        folderManager,
                        section
                );

        var header = new JPanel();

        header.setLayout(
                new BoxLayout(
                        header,
                        BoxLayout.Y_AXIS
                )
        );

        header.setOpaque(false);

        if (originalNorth != null)
        {
            var originalToolbarWrapper =
                    new JPanel(new BorderLayout());

            originalToolbarWrapper.setOpaque(false);

            originalToolbarWrapper.add(
                    originalNorth,
                    BorderLayout.CENTER
            );

            header.add(originalToolbarWrapper);
        }

        var folderPanelWrapper =
                new JPanel(new BorderLayout());

        folderPanelWrapper.setOpaque(false);

        folderPanelWrapper.add(
                folderPanel,
                BorderLayout.CENTER
        );

        header.add(folderPanelWrapper);

        resourcePanel.add(
                header,
                BorderLayout.NORTH
        );

        if (!(resourcePanel instanceof IReloadableFilterable reloadable))
        {
            LOG.warn(
                    "Resource Folders: {} is not reloadable",
                    resourcePanel.getClass().getName()
            );

            return;
        }

        Runnable reloadSection = () ->
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                reloadable.reloadElements();
            }
            else
            {
                SwingUtilities.invokeLater(
                        reloadable::reloadElements
                );
            }
        };

        var importTracker =
                new ResourceImportTracker(
                        mcreator.getWorkspace(),
                        folderManager,
                        section,
                        folderPanel,
                        () -> getSectionResources(
                                mcreator,
                                section
                        ),
                        () ->
                                resourcePanel.isShowing()
                                        && resourceTabs.getSelectedComponent()
                                        == resourcePanel,
                        reloadSection
                );

        resourcePanel.putClientProperty(
                "resourceFolders.importTracker",
                importTracker
        );

        if (section == ResourceSection.TEXTURES)
        {
            installTextureFilters(
                    mcreator,
                    resourceLists,
                    folderPanel,
                    folderManager,
                    section,
                    reloadSection
            );
        }
        else
        {
            installStandardFilters(
                    mcreator,
                    resourceLists,
                    folderPanel,
                    folderManager,
                    section,
                    reloadSection
            );
        }

        folderPanel.addFolderChangedListener(_ ->
        {
            reloadSection.run();
            importTracker.resetBaseline();
        });

        resourcePanel.revalidate();
        resourcePanel.repaint();

        LOG.info(
                "Resource Folders: installed {} section",
                section.getDisplayName()
        );
    }

    private static Collection<?> getSectionResources(
            ModMaker mcreator,
            ResourceSection section)
    {
        return switch (section)
        {
            case TEXTURES ->
            {
                var textures =
                        new ArrayList<File>();

                for (var textureType : TextureType.values())
                {
                    textures.addAll(
                            mcreator
                                    .getFolderManager()
                                    .getTexturesList(textureType)
                    );
                }

                yield textures;
            }

            case SOUNDS ->
                    mcreator
                            .getWorkspace()
                            .getSoundElements();

            case MODELS ->
                    Model.getModels(
                            mcreator.getWorkspace()
                    );

            case ANIMATIONS ->
                    Animation.getAnimations(
                            mcreator.getWorkspace()
                    );

            case STRUCTURES ->
                    mcreator
                            .getFolderManager()
                            .getStructureList();

            case SCREENSHOTS ->
            {
                var screenshotsDirectory =
                        new File(
                                mcreator
                                        .getFolderManager()
                                        .getClientRunDir(),
                                "screenshots"
                        );

                var screenshots =
                        screenshotsDirectory.listFiles();

                yield screenshots != null
                        ? List.of(screenshots)
                        : List.of();
            }
        };
    }

    private static JTabbedPane findResourceTabs(
            Container resourcesPanel)
    {
        for (var component :
                resourcesPanel.getComponents())
        {
            if (component instanceof JTabbedPane tabs)
            {
                return tabs;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static void installStandardFilters(
            ModMaker mcreator,
            List<JList<?>> resourceLists,
            ResourceFolderPanel folderPanel,
            ResourceFolderManager folderManager,
            ResourceSection section,
            Runnable reloadSection)
    {
        for (var resourceList : resourceLists)
        {
            var list =
                    (JList<Object>) resourceList;

            var originalModel =
                    (ListModel<Object>) list.getModel();

            var folderModel =
                    new FolderFilteredListModel<>(
                            originalModel,
                            resource ->
                                    isInCurrentFolder(
                                            resource,
                                            mcreator,
                                            folderPanel,
                                            folderManager,
                                            section
                                    )
                    );

            list.setModel(folderModel);

            ResourceMoveMenuInstaller.install(
                    list,
                    mcreator.getWorkspace(),
                    folderManager,
                    section,
                    reloadSection
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static void installTextureFilters(
            ModMaker mcreator,
            List<JList<?>> resourceLists,
            ResourceFolderPanel folderPanel,
            ResourceFolderManager folderManager,
            ResourceSection section,
            Runnable reloadSection)
    {
        for (var resourceList : resourceLists)
        {
            var list =
                    (JList<File>) resourceList;

            var folderModel =
                    new ResourceFolderFilterModel<File>(
                            mcreator.getWorkspacePanel(),
                            File::getName,
                            resource ->
                                    isInCurrentFolder(
                                            resource,
                                            mcreator,
                                            folderPanel,
                                            folderManager,
                                            section
                                    )
                    );

            list.setModel(folderModel);

            ResourceMoveMenuInstaller.install(
                    list,
                    mcreator.getWorkspace(),
                    folderManager,
                    section,
                    reloadSection
            );
        }

        reloadSection.run();
    }

    private static boolean isInCurrentFolder(
            Object resource,
            ModMaker mcreator,
            ResourceFolderPanel folderPanel,
            ResourceFolderManager folderManager,
            ResourceSection section)
    {
        var resourceKey =
                ResourceKey.of(
                        resource,
                        mcreator.getWorkspace()
                );

        var resourceFolderId =
                folderManager.getResourceFolder(
                        section.getId(),
                        resourceKey
                );

        return resourceFolderId.equals(
                folderPanel.getCurrentFolderId()
        );
    }

    private static List<JList<?>> findResourceLists(
            Container container)
    {
        var lists =
                new ArrayList<JList<?>>();

        for (var component :
                container.getComponents())
        {
            if (component instanceof JList<?> list)
            {
                lists.add(list);
            }

            if (component instanceof Container child)
            {
                lists.addAll(
                        findResourceLists(child)
                );
            }
        }

        return lists;
    }
}