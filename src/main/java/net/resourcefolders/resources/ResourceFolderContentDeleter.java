package net.resourcefolders.resources;

import net.mcreator.ui.dialogs.imageeditor.NewImageDialog;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.ui.views.editor.image.metadata.MetadataManager;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.resources.Animation;
import net.mcreator.workspace.resources.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public final class ResourceFolderContentDeleter
{
    private ResourceFolderContentDeleter()
    {
    }

    public static void deleteResources(
            ModMaker mcreator,
            ResourceSection section,
            Set<String> resourceKeys)
    {
        if (resourceKeys.isEmpty())
        {
            return;
        }

        switch (section)
        {
            case TEXTURES ->
                    deleteTextures(
                            mcreator,
                            resourceKeys
                    );

            case SOUNDS ->
                    deleteSounds(
                            mcreator,
                            resourceKeys
                    );

            case MODELS ->
                    deleteModels(
                            mcreator,
                            resourceKeys
                    );

            case ANIMATIONS ->
                    deleteAnimations(
                            mcreator,
                            resourceKeys
                    );

            case STRUCTURES ->
                    deleteStructures(
                            mcreator,
                            resourceKeys
                    );

            case SCREENSHOTS ->
                    deleteScreenshots(
                            mcreator,
                            resourceKeys
                    );
        }
    }

    private static void deleteTextures(
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        var workspace =
                mcreator.getWorkspace();

        for (var textureType :
                TextureType.getSupportedTypes(
                        workspace,
                        true))
        {
            var textures =
                    new ArrayList<>(
                            mcreator
                                    .getFolderManager()
                                    .getTexturesList(
                                            textureType
                                    )
                    );

            for (var texture : textures)
            {
                if (!matches(
                        texture,
                        mcreator,
                        resourceKeys))
                {
                    continue;
                }

                var editorMetadata =
                        MetadataManager.getMetadataFile(
                                workspace,
                                texture
                        );

                texture.delete();

                var mcmeta =
                        new File(
                                texture.getAbsolutePath()
                                        + ".mcmeta"
                        );

                if (mcmeta.isFile())
                {
                    mcmeta.delete();
                }

                if (editorMetadata.isFile())
                {
                    editorMetadata.delete();
                }
            }
        }
    }

    private static void deleteSounds(
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        var workspace =
                mcreator.getWorkspace();

        var sounds =
                new ArrayList<>(
                        workspace.getSoundElements()
                );

        for (var sound : sounds)
        {
            if (matches(
                    sound,
                    mcreator,
                    resourceKeys))
            {
                workspace.removeSoundElement(
                        sound
                );
            }
        }
    }

    private static void deleteModels(
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        var models =
                Model.getModels(
                        mcreator.getWorkspace()
                );

        for (var model : models)
        {
            if (!matches(
                    model,
                    mcreator,
                    resourceKeys))
            {
                continue;
            }

            var files =
                    model.getFiles();

            if (files == null)
            {
                continue;
            }

            Arrays.stream(files)
                    .filter(file ->
                            file != null
                                    && file.isFile()
                    )
                    .forEach(File::delete);
        }
    }

    private static void deleteAnimations(
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        var animations =
                Animation.getAnimations(
                        mcreator.getWorkspace()
                );

        for (var animation : animations)
        {
            if (!matches(
                    animation,
                    mcreator,
                    resourceKeys))
            {
                continue;
            }

            var file =
                    animation.getFile();

            if (file != null)
            {
                file.delete();
            }
        }
    }

    private static void deleteStructures(
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        var structures =
                new ArrayList<>(
                        mcreator
                                .getFolderManager()
                                .getStructureList()
                );

        for (var structure : structures)
        {
            if (matches(
                    structure,
                    mcreator,
                    resourceKeys))
            {
                mcreator
                        .getFolderManager()
                        .removeStructure(
                                structure
                        );
            }
        }
    }

    private static void deleteScreenshots(
            ModMaker mcreator,
            Set<String> resourceKeys)
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

        if (screenshots == null)
        {
            return;
        }

        for (var screenshot : screenshots)
        {
            if (matches(
                    screenshot,
                    mcreator,
                    resourceKeys))
            {
                screenshot.delete();
            }
        }
    }

    private static boolean matches(
            Object resource,
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        try
        {
            var resourceKey =
                    ResourceKey.of(
                            resource,
                            mcreator.getWorkspace()
                    );

            return resourceKeys.contains(
                    resourceKey
            );
        }
        catch (IllegalArgumentException ignored)
        {
            return false;
        }
    }
}