package net.resourcefolders.resources;

import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.ui.views.editor.image.metadata.MetadataManager;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.elements.SoundElement;
import net.mcreator.workspace.resources.Animation;
import net.mcreator.workspace.resources.Model;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ResourceFolderContentDeleter
{
    private static final String STAGING_DIRECTORY_NAME =
            "resource-folders-delete";

    private ResourceFolderContentDeleter()
    {
    }

    public static DeletionResult deleteResources(
            ModMaker mcreator,
            ResourceSection section,
            Set<String> resourceKeys)
    {
        if (resourceKeys.isEmpty())
        {
            return DeletionResult.success();
        }

        final DeletionPlan deletionPlan;

        try
        {
            deletionPlan =
                    createDeletionPlan(
                            mcreator,
                            section,
                            resourceKeys
                    );
        }
        catch (RuntimeException exception)
        {
            return DeletionResult.failure(
                    List.of(
                            "Could not inspect all resources: "
                                    + getExceptionMessage(
                                    exception
                            )
                    )
            );
        }

        if (deletionPlan.files().isEmpty()
                && deletionPlan.sounds().isEmpty())
        {
            return DeletionResult.success();
        }

        var stagingDirectory =
                mcreator
                        .getWorkspace()
                        .getFolderManager()
                        .getWorkspaceCacheDir()
                        .toPath()
                        .resolve(STAGING_DIRECTORY_NAME)
                        .resolve(UUID.randomUUID().toString());

        var stagedFiles =
                new ArrayList<StagedFile>();

        try
        {
            Files.createDirectories(
                    stagingDirectory
            );

            stageFiles(
                    deletionPlan.files(),
                    stagingDirectory,
                    stagedFiles
            );
        }
        catch (IOException | SecurityException exception)
        {
            var messages =
                    new ArrayList<String>();

            messages.add(
                    "Could not prepare all resources for deletion: "
                            + getExceptionMessage(
                            exception
                    )
            );

            messages.addAll(
                    restoreStagedFiles(
                            stagedFiles,
                            stagingDirectory
                    )
            );

            return DeletionResult.failure(
                    messages
            );
        }

        var removedSounds =
                new ArrayList<SoundElement>();

        try
        {
            for (var sound : deletionPlan.sounds())
            {
                mcreator
                        .getWorkspace()
                        .removeSoundElement(
                                sound
                        );

                removedSounds.add(
                        sound
                );
            }
        }
        catch (RuntimeException exception)
        {
            var messages =
                    new ArrayList<String>();

            messages.add(
                    "Could not update the workspace sound registry: "
                            + getExceptionMessage(
                            exception
                    )
            );

            messages.addAll(
                    restoreStagedFiles(
                            stagedFiles,
                            stagingDirectory
                    )
            );

            for (var sound : removedSounds)
            {
                mcreator
                        .getWorkspace()
                        .addSoundElement(
                                sound
                        );
            }

            return DeletionResult.failure(
                    messages
            );
        }

        return DeletionResult.success(
                deleteStagedFiles(
                        stagedFiles,
                        stagingDirectory
                )
        );
    }

    private static DeletionPlan createDeletionPlan(
            ModMaker mcreator,
            ResourceSection section,
            Set<String> resourceKeys)
    {
        var files =
                new LinkedHashSet<Path>();

        var sounds =
                new ArrayList<SoundElement>();

        switch (section)
        {
            case TEXTURES ->
                    collectTextures(
                            mcreator,
                            resourceKeys,
                            files
                    );

            case SOUNDS ->
                    collectSounds(
                            mcreator,
                            resourceKeys,
                            files,
                            sounds
                    );

            case MODELS ->
                    collectModels(
                            mcreator,
                            resourceKeys,
                            files
                    );

            case ANIMATIONS ->
                    collectAnimations(
                            mcreator,
                            resourceKeys,
                            files
                    );

            case STRUCTURES ->
                    collectStructures(
                            mcreator,
                            resourceKeys,
                            files
                    );

            case SCREENSHOTS ->
                    collectScreenshots(
                            mcreator,
                            resourceKeys,
                            files
                    );
        }

        return new DeletionPlan(
                List.copyOf(files),
                List.copyOf(sounds)
        );
    }

    private static void collectTextures(
            ModMaker mcreator,
            Set<String> resourceKeys,
            Set<Path> files)
    {
        var workspace =
                mcreator.getWorkspace();

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
                if (!matches(
                        texture,
                        mcreator,
                        resourceKeys))
                {
                    continue;
                }

                addIfPresent(
                        files,
                        texture
                );

                addIfPresent(
                        files,
                        new File(
                                texture.getAbsolutePath()
                                        + ".mcmeta"
                        )
                );

                addIfPresent(
                        files,
                        MetadataManager.getMetadataFile(
                                workspace,
                                texture
                        )
                );
            }
        }
    }

    private static void collectSounds(
            ModMaker mcreator,
            Set<String> resourceKeys,
            Set<Path> files,
            List<SoundElement> sounds)
    {
        var workspace =
                mcreator.getWorkspace();

        var soundsDirectory =
                mcreator
                        .getFolderManager()
                        .getSoundsDir();

        for (var sound :
                new ArrayList<>(
                        workspace.getSoundElements()
                ))
        {
            if (!matches(
                    sound,
                    mcreator,
                    resourceKeys))
            {
                continue;
            }

            sounds.add(
                    sound
            );

            if (soundsDirectory != null)
            {
                for (var soundFile : sound.getFiles())
                {
                    addIfPresent(
                            files,
                            new File(
                                    soundsDirectory,
                                    soundFile + ".ogg"
                            )
                    );
                }
            }
        }
    }

    private static void collectModels(
            ModMaker mcreator,
            Set<String> resourceKeys,
            Set<Path> files)
    {
        for (var model :
                Model.getModels(
                        mcreator.getWorkspace()
                ))
        {
            if (!matches(
                    model,
                    mcreator,
                    resourceKeys))
            {
                continue;
            }

            var modelFiles =
                    model.getFiles();

            if (modelFiles == null)
            {
                continue;
            }

            for (var modelFile : modelFiles)
            {
                addIfPresent(
                        files,
                        modelFile
                );
            }
        }
    }

    private static void collectAnimations(
            ModMaker mcreator,
            Set<String> resourceKeys,
            Set<Path> files)
    {
        for (var animation :
                Animation.getAnimations(
                        mcreator.getWorkspace()
                ))
        {
            if (matches(
                    animation,
                    mcreator,
                    resourceKeys))
            {
                addIfPresent(
                        files,
                        animation.getFile()
                );
            }
        }
    }

    private static void collectStructures(
            ModMaker mcreator,
            Set<String> resourceKeys,
            Set<Path> files)
    {
        var structuresDirectory =
                mcreator
                        .getFolderManager()
                        .getStructuresDir();

        if (structuresDirectory == null)
        {
            return;
        }

        var structureExtension =
                mcreator
                        .getWorkspace()
                        .getGeneratorConfiguration()
                        .getStructureExtension();

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
                addIfPresent(
                        files,
                        new File(
                                structuresDirectory,
                                structure
                                        + "."
                                        + structureExtension
                        )
                );
            }
        }
    }

    private static void collectScreenshots(
            ModMaker mcreator,
            Set<String> resourceKeys,
            Set<Path> files)
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
                addIfPresent(
                        files,
                        screenshot
                );
            }
        }
    }

    private static void addIfPresent(
            Set<Path> files,
            File file)
    {
        if (file == null)
        {
            return;
        }

        var path =
                file
                        .toPath()
                        .toAbsolutePath()
                        .normalize();

        if (Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS))
        {
            files.add(
                    path
            );
        }
    }

    private static void stageFiles(
            List<Path> files,
            Path stagingDirectory,
            List<StagedFile> stagedFiles)
            throws IOException
    {
        for (int i = 0;
             i < files.size();
             i++)
        {
            var originalPath =
                    files.get(i);

            var stagedPath =
                    stagingDirectory.resolve(
                            Integer.toString(i)
                    );

            if (!Files.getFileStore(
                    originalPath
            ).equals(Files.getFileStore(
                    stagingDirectory
            )))
            {
                throw new IOException(
                        "Resource is on a different filesystem: "
                                + originalPath
                );
            }

            move(
                    originalPath,
                    stagedPath
            );

            stagedFiles.add(
                    new StagedFile(
                            originalPath,
                            stagedPath
                    )
            );
        }
    }

    private static List<String> restoreStagedFiles(
            List<StagedFile> stagedFiles,
            Path stagingDirectory)
    {
        var messages =
                new ArrayList<String>();

        for (int i = stagedFiles.size() - 1;
             i >= 0;
             i--)
        {
            var stagedFile =
                    stagedFiles.get(i);

            try
            {
                move(
                        stagedFile.stagedPath(),
                        stagedFile.originalPath()
                );
            }
            catch (IOException | SecurityException exception)
            {
                messages.add(
                        "Could not restore "
                                + stagedFile.originalPath()
                                + " from "
                                + stagedFile.stagedPath()
                                + ": "
                                + getExceptionMessage(
                                exception
                        )
                );
            }
        }

        if (messages.isEmpty())
        {
            messages.addAll(
                    deleteEmptyStagingDirectory(
                            stagingDirectory
                    )
            );
        }

        return messages;
    }

    private static List<String> deleteStagedFiles(
            List<StagedFile> stagedFiles,
            Path stagingDirectory)
    {
        var messages =
                new ArrayList<String>();

        for (var stagedFile : stagedFiles)
        {
            try
            {
                Files.deleteIfExists(
                        stagedFile.stagedPath()
                );
            }
            catch (IOException | SecurityException exception)
            {
                messages.add(
                        "A staged copy of "
                                + stagedFile.originalPath()
                                + " at "
                                + stagedFile.stagedPath()
                                + " could not be permanently deleted: "
                                + getExceptionMessage(
                                exception
                        )
                );
            }
        }

        if (messages.isEmpty())
        {
            messages.addAll(
                    deleteEmptyStagingDirectory(
                            stagingDirectory
                    )
            );
        }

        return messages;
    }

    private static List<String> deleteEmptyStagingDirectory(
            Path stagingDirectory)
    {
        var messages =
                new ArrayList<String>();

        try
        {
            Files.deleteIfExists(
                    stagingDirectory
            );
        }
        catch (IOException | SecurityException exception)
        {
            messages.add(
                    "Could not remove temporary directory "
                            + stagingDirectory
                            + ": "
                            + getExceptionMessage(
                            exception
                    )
            );
        }

        return messages;
    }

    private static void move(
            Path source,
            Path target)
            throws IOException
    {
        try
        {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE
            );
        }
        catch (AtomicMoveNotSupportedException ignored)
        {
            Files.move(
                    source,
                    target
            );
        }
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

    private static String getExceptionMessage(
            Exception exception)
    {
        var message =
                exception.getMessage();

        return message != null
                ? message
                : exception
                .getClass()
                .getSimpleName();
    }

    public record DeletionResult(
            boolean successful,
            List<String> messages)
    {
        public DeletionResult
        {
            messages =
                    List.copyOf(messages);
        }

        private static DeletionResult success()
        {
            return success(
                    List.of()
            );
        }

        private static DeletionResult success(
                List<String> messages)
        {
            return new DeletionResult(
                    true,
                    messages
            );
        }

        private static DeletionResult failure(
                List<String> messages)
        {
            return new DeletionResult(
                    false,
                    messages
            );
        }
    }

    private record DeletionPlan(
            List<Path> files,
            List<SoundElement> sounds)
    {
    }

    private record StagedFile(
            Path originalPath,
            Path stagedPath)
    {
    }
}
