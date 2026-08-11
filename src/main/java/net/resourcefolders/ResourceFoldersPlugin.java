package net.resourcefolders;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.resourcefolders.ui.ResourceFoldersInstaller;

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

                ResourceFoldersInstaller.install(modMaker);
            });
        });
    }
}