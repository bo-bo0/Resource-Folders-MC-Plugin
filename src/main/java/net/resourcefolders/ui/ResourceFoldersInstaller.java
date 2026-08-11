package net.resourcefolders.ui;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.resourcefolders.folders.ResourceFolderManager;
import net.resourcefolders.resources.ResourceSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

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

        resourcePanel.revalidate();
        resourcePanel.repaint();

        LOG.info(
                "Resource Folders: installed {} section",
                section.getDisplayName()
        );
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
}