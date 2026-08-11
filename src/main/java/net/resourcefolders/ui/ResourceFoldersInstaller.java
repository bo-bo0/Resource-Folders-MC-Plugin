package net.resourcefolders.ui;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.ui.workspace.resources.ResourceFilterModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class ResourceFoldersInstaller
{
    private static final Logger LOG =
            LogManager.getLogger("Resource Folders plugin");

    private ResourceFoldersInstaller()
    {
    }

    public static void install(ModMaker mcreator)
    {
        var workspacePanel = mcreator.getWorkspacePanel();
        var resourcesPanel = workspacePanel.resourcesPan;

        var resourceLists = findResourceLists(resourcesPanel);

        LOG.info(
                "Resource Folders: found {} resource lists",
                resourceLists.size()
        );

        for (int i = 0; i < resourceLists.size(); i++)
        {
            var list = resourceLists.get(i);

            LOG.info(
                    "Resource Folders: list {} -> {}, model = {}",
                    i,
                    list.getClass().getName(),
                    list.getModel().getClass().getName()
            );

            if (list.getModel() instanceof ResourceFilterModel<?>)
            {
                LOG.info(
                        "Resource Folders: list {} uses ResourceFilterModel",
                        i
                );
            }
        }
    }

    private static List<JList<?>> findResourceLists(Container container)
    {
        var lists = new ArrayList<JList<?>>();

        for (var component : container.getComponents())
        {
            if (component instanceof JList<?> list)
            {
                lists.add(list);
            }

            if (component instanceof Container child)
            {
                lists.addAll(findResourceLists(child));
            }
        }

        return lists;
    }
}