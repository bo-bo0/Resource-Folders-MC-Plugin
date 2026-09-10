package net.resourcefolders.resources.deletion;

import net.mcreator.workspace.elements.SoundElement;

import java.nio.file.Path;
import java.util.List;

record DeletionPlan(
        List<Path> files,
        List<SoundElement> sounds)
{
}
