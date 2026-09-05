package net.resourcefolders.resources;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.references.ReferencesFinder;
import net.mcreator.workspace.resources.Animation;
import net.mcreator.workspace.resources.Model;

import java.util.HashSet;
import java.util.Set;

public final class ResourceFolderUsageFinder
{
    private ResourceFolderUsageFinder()
    {
    }

    public static Set<ModElement> findUsages(
            ModMaker mcreator,
            ResourceSection section,
            Set<String> resourceKeys)
    {
        var references =
                new HashSet<ModElement>();

        if (resourceKeys.isEmpty())
        {
            return references;
        }

        var workspace =
                mcreator.getWorkspace();

        switch (section)
        {
            case TEXTURES ->
            {
                for (var textureType :
                        TextureType.getSupportedTypes(
                                workspace,
                                true))
                {
                    for (var texture :
                            mcreator
                                    .getFolderManager()
                                    .getTexturesList(
                                            textureType
                                    ))
                    {
                        if (matches(
                                texture,
                                mcreator,
                                resourceKeys))
                        {
                            references.addAll(
                                    ReferencesFinder
                                            .searchTextureUsages(
                                                    workspace,
                                                    texture,
                                                    textureType
                                            )
                            );
                        }
                    }
                }
            }

            case SOUNDS ->
            {
                for (var sound :
                        workspace.getSoundElements())
                {
                    if (matches(
                            sound,
                            mcreator,
                            resourceKeys))
                    {
                        references.addAll(
                                ReferencesFinder
                                        .searchSoundUsages(
                                                workspace,
                                                sound
                                        )
                        );
                    }
                }
            }

            case MODELS ->
            {
                for (var model :
                        Model.getModels(workspace))
                {
                    if (matches(
                            model,
                            mcreator,
                            resourceKeys))
                    {
                        references.addAll(
                                ReferencesFinder
                                        .searchModelUsages(
                                                workspace,
                                                model
                                        )
                        );
                    }
                }
            }

            case ANIMATIONS ->
            {
                for (var animation :
                        Animation.getAnimations(
                                workspace))
                {
                    if (matches(
                            animation,
                            mcreator,
                            resourceKeys))
                    {
                        references.addAll(
                                ReferencesFinder
                                        .searchAnimationUsages(
                                                workspace,
                                                animation
                                        )
                        );
                    }
                }
            }

            case STRUCTURES ->
            {
                for (var structure :
                        mcreator
                                .getFolderManager()
                                .getStructureList())
                {
                    if (matches(
                            structure,
                            mcreator,
                            resourceKeys))
                    {
                        references.addAll(
                                ReferencesFinder
                                        .searchStructureUsages(
                                                workspace,
                                                structure
                                        )
                        );
                    }
                }
            }

            case SCREENSHOTS ->
            {
            }
        }

        return references;
    }

    private static boolean matches(
            Object resource,
            ModMaker mcreator,
            Set<String> resourceKeys)
    {
        try
        {
            return resourceKeys.contains(
                    ResourceKey.of(
                            resource,
                            mcreator.getWorkspace()
                    )
            );
        }
        catch (IllegalArgumentException ignored)
        {
            return false;
        }
    }
}
