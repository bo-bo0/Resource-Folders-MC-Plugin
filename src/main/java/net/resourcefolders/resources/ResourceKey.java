package net.resourcefolders.resources;

import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.SoundElement;
import net.mcreator.workspace.resources.Animation;
import net.mcreator.workspace.resources.Model;

import java.io.File;

public final class ResourceKey
{
    private ResourceKey()
    {
    }

    public static String of(Object resource, Workspace workspace)
    {
        if (resource instanceof File file)
        {
            return "file:" + getFileKey(file, workspace);
        }

        if (resource instanceof SoundElement sound)
        {
            return "sound:" + sound.getName();
        }

        if (resource instanceof Model model)
        {
            return "model:" + getFileKey(model.getFile(), workspace);
        }

        if (resource instanceof Animation animation)
        {
            return "animation:" + getFileKey(animation.getFile(), workspace);
        }

        if (resource instanceof String structure)
        {
            return "structure:" + structure;
        }

        throw new IllegalArgumentException(
                "Unsupported resource type: " + resource.getClass().getName()
        );
    }

    private static String getFileKey(File file, Workspace workspace)
    {
        try
        {
            return workspace.getFolderManager()
                    .getPathInWorkspace(file)
                    .replace('\\', '/');
        }
        catch (Exception ignored)
        {
            return file.getAbsolutePath().replace('\\', '/');
        }
    }
}