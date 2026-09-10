package net.resourcefolders.resources.deletion;

import java.nio.file.Path;

record StagedFile(
        Path originalPath,
        Path stagedPath)
{
}
