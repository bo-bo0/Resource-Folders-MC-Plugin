package net.resourcefolders.resources;

import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.TagElement;
import net.mcreator.workspace.elements.VariableElement;
import net.mcreator.workspace.elements.SoundElement;
import net.mcreator.workspace.resources.Animation;
import net.mcreator.workspace.resources.Model;

import java.io.File;

public final class ResourceKey
{
    private ResourceKey()
    {
    }

    public static String of(
            Object resource,
            Workspace workspace)
    {
        if (resource instanceof TagElement tag)
        {
            return ofTag(tag);
        }
        if (resource instanceof VariableElement variable)
        {
            return ofVariable(variable.getName());
        }
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
            if (model.getFile() != null)
            {
                return "model:" + getFileKey(
                        model.getFile(),
                        workspace
                );
            }

            return "model:"
                    + model.getType()
                    + ":"
                    + model.getReadableName();
        }

        if (resource instanceof Animation animation)
        {
            return "animation:" + getFileKey(
                    animation.getFile(),
                    workspace
            );
        }

        if (resource instanceof String structure)
        {
            return "structure:" + structure;
        }

        throw new IllegalArgumentException(
                "Unsupported resource type: "
                        + resource.getClass().getName()
        );
    }

    public static String ofTag(TagElement tag)
    {
        return "tag:" + tag.type().name() + ":"
                + tag.getMCreatorNamespace() + ":" + tag.getName();
    }

    public static String ofVariable(String name)
    {
        return "variable:" + name;
    }

    private static String getFileKey(
            File file,
            Workspace workspace)
    {
        try
        {
            return workspace
                    .getFolderManager()
                    .getPathInWorkspace(file)
                    .replace('\\', '/');
        }
        catch (RuntimeException ignored)
        {
            return file
                    .getAbsolutePath()
                    .replace('\\', '/');
        }
    }
}
