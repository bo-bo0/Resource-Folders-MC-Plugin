package net.resourcefolders.ui;

import net.mcreator.ui.workspace.WorkspacePanel;
import net.mcreator.ui.workspace.resources.ResourceFilterModel;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ResourceFolderFilterModel<T>
        extends ResourceFilterModel<T>
{
    public ResourceFolderFilterModel(
            WorkspacePanel workspacePanel,
            Function<T, String> resourceNameSupplier,
            Predicate<T> folderFilter)
    {
        super(
                workspacePanel,
                (item, query) ->
                        folderFilter.test(item)
                                && resourceNameSupplier
                                .apply(item)
                                .toLowerCase(Locale.ENGLISH)
                                .contains(query),
                resourceNameSupplier
        );
    }
}