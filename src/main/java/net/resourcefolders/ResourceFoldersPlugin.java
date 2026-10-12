package net.resourcefolders;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.resourcefolders.ui.ResourceFoldersInstaller;
import net.resourcefolders.ui.TagFoldersInstaller;
import net.resourcefolders.folders.ResourceFolderManager;

import javax.swing.*;

public class ResourceFoldersPlugin extends JavaPlugin
{
    public ResourceFoldersPlugin(Plugin plugin)
    {
        super(plugin);

        addListener(MCreatorLoadedEvent.class, event ->
        {
            SwingUtilities.invokeLater(() ->
            {
                if (!(event.getMCreator() instanceof ModMaker modMaker))
                {
                    return;
                }

                var folderManager = new ResourceFolderManager(modMaker.getWorkspace());
                ResourceFoldersInstaller.install(modMaker, folderManager);
                TagFoldersInstaller.install(modMaker, folderManager);
            });
        });
    }
}
