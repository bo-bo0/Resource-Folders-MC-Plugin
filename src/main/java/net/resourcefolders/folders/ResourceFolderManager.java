package net.resourcefolders.folders;

import com.google.gson.Gson;
import net.mcreator.workspace.Workspace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResourceFolderManager
{
    private static final String METADATA_KEY =
            "resource_folders";

    private static final Gson GSON =
            new Gson();

    private final Workspace workspace;

    private final Map<String, SectionIndex> sectionIndexes =
            new HashMap<>();

    private ResourceFolderData data;

    public ResourceFolderManager(Workspace workspace)
    {
        this.workspace = workspace;
        this.data = load();
    }

    public List<ResourceFolder> getChildren(
            String sectionId,
            String parentId)
    {
        return getSectionIndex(sectionId)
                .childrenByParentId()
                .getOrDefault(
                        parentId,
                        List.of()
                );
    }

    public ResourceFolder getFolder(
            String sectionId,
            String folderId)
    {
        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            return null;
        }

        return getSectionIndex(sectionId)
                .foldersById()
                .get(folderId);
    }

    public ResourceFolder createFolder(
            String sectionId,
            String name,
            String parentId)
    {
        var folder =
                new ResourceFolder(
                        name,
                        parentId
                );

        getSection(sectionId)
                .getFolders()
                .add(folder);

        invalidateSectionIndex(sectionId);

        save();

        return folder;
    }

    public void renameFolder(
            String sectionId,
            String folderId,
            String newName)
    {
        var folder =
                getFolder(
                        sectionId,
                        folderId
                );

        if (folder == null)
        {
            return;
        }

        folder.setName(newName);

        invalidateSectionIndex(sectionId);

        save();
    }

    public boolean folderExists(
            String sectionId,
            String name,
            String parentId)
    {
        return folderExists(
                sectionId,
                name,
                parentId,
                null
        );
    }

    public boolean folderExists(
            String sectionId,
            String name,
            String parentId,
            String excludedFolderId)
    {
        return getChildren(
                sectionId,
                parentId
        )
                .stream()
                .filter(folder ->
                        excludedFolderId == null
                                || !folder.getId()
                                .equals(excludedFolderId)
                )
                .anyMatch(folder ->
                        folder.getName()
                                .equalsIgnoreCase(name)
                );
    }

    public String getParentId(
            String sectionId,
            String folderId)
    {
        var folder =
                getFolder(
                        sectionId,
                        folderId
                );

        if (folder == null
                || folder.getParentId() == null)
        {
            return ResourceFolderData.ROOT_ID;
        }

        return folder.getParentId();
    }

    public String getResourceFolder(
            String sectionId,
            String resourceKey)
    {
        return getSection(sectionId)
                .getResourceFolders()
                .getOrDefault(
                        resourceKey,
                        ResourceFolderData.ROOT_ID
                );
    }

    public void moveResources(
            String sectionId,
            java.util.Collection<String> resourceKeys,
            String folderId)
    {
        var resourceFolders =
                getSection(sectionId)
                        .getResourceFolders();

        for (var resourceKey : resourceKeys)
        {
            if (ResourceFolderData.ROOT_ID.equals(folderId))
            {
                resourceFolders.remove(
                        resourceKey
                );
            }
            else
            {
                resourceFolders.put(
                        resourceKey,
                        folderId
                );
            }
        }

        save();
    }

    public void removeMissingResourceMappings(
            String sectionId,
            Set<String> existingResourceKeys)
    {
        var resourceFolders =
                getSection(sectionId)
                        .getResourceFolders();

        boolean changed =
                resourceFolders
                        .keySet()
                        .removeIf(resourceKey ->
                                !existingResourceKeys.contains(
                                        resourceKey
                                )
                        );

        if (changed)
        {
            save();
        }
    }

    public Set<String> getFolderTreeIds(
            String sectionId,
            String folderId)
    {
        var result =
                new HashSet<String>();

        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            return result;
        }

        if (getFolder(
                sectionId,
                folderId) == null)
        {
            return result;
        }

        var pending =
                new ArrayDeque<String>();

        pending.add(folderId);

        while (!pending.isEmpty())
        {
            var currentId =
                    pending.removeFirst();

            if (!result.add(currentId))
            {
                continue;
            }

            for (var folder :
                    getChildren(
                            sectionId,
                            currentId))
            {
                pending.addLast(
                        folder.getId()
                );
            }
        }

        return result;
    }

    public int getFolderTreeSize(
            String sectionId,
            String folderId)
    {
        return getFolderTreeIds(
                sectionId,
                folderId
        ).size();
    }

    public Set<String> getResourceKeysInFolderTree(
            String sectionId,
            String folderId)
    {
        var folderIds =
                getFolderTreeIds(
                        sectionId,
                        folderId
                );

        var resourceKeys =
                new HashSet<String>();

        for (var entry :
                getSection(sectionId)
                        .getResourceFolders()
                        .entrySet())
        {
            if (folderIds.contains(
                    entry.getValue()))
            {
                resourceKeys.add(
                        entry.getKey()
                );
            }
        }

        return resourceKeys;
    }

    public void deleteFolderTree(
            String sectionId,
            String folderId)
    {
        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            return;
        }

        var folderIds =
                getFolderTreeIds(
                        sectionId,
                        folderId
                );

        if (folderIds.isEmpty())
        {
            return;
        }

        var section =
                getSection(sectionId);

        section
                .getFolders()
                .removeIf(folder ->
                        folderIds.contains(
                                folder.getId()
                        )
                );

        section
                .getResourceFolders()
                .entrySet()
                .removeIf(entry ->
                        folderIds.contains(
                                entry.getValue()
                        )
                );

        invalidateSectionIndex(sectionId);

        save();
    }

    private SectionIndex getSectionIndex(
            String sectionId)
    {
        return sectionIndexes.computeIfAbsent(
                sectionId,
                _ ->
                        SectionIndex.create(
                                getSection(sectionId)
                        )
        );
    }

    private void invalidateSectionIndex(
            String sectionId)
    {
        sectionIndexes.remove(sectionId);
    }

    private ResourceSectionData getSection(
            String sectionId)
    {
        return data
                .getSections()
                .computeIfAbsent(
                        sectionId,
                        _ ->
                                new ResourceSectionData()
                );
    }

    private ResourceFolderData load()
    {
        var storedData =
                workspace.getMetadata(
                        METADATA_KEY
                );

        if (!(storedData instanceof String json)
                || json.isBlank())
        {
            return new ResourceFolderData();
        }

        try
        {
            var loadedData =
                    GSON.fromJson(
                            json,
                            ResourceFolderData.class
                    );

            return loadedData != null
                    ? loadedData
                    : new ResourceFolderData();
        }
        catch (Exception ignored)
        {
            return new ResourceFolderData();
        }
    }

    private void save()
    {
        workspace.putMetadata(
                METADATA_KEY,
                GSON.toJson(data)
        );

        workspace.markDirty();
    }

    private record SectionIndex(
            Map<String, ResourceFolder> foldersById,
            Map<String, List<ResourceFolder>> childrenByParentId)
    {
        private static final Comparator<ResourceFolder>
                FOLDER_NAME_COMPARATOR =
                Comparator.comparing(
                        ResourceFolder::getName,
                        String.CASE_INSENSITIVE_ORDER
                );

        private static SectionIndex create(
                ResourceSectionData section)
        {
            var foldersById =
                    new HashMap<String, ResourceFolder>();

            var mutableChildrenByParentId =
                    new HashMap<String, List<ResourceFolder>>();

            for (var folder : section.getFolders())
            {
                foldersById.putIfAbsent(
                        folder.getId(),
                        folder
                );

                mutableChildrenByParentId
                        .computeIfAbsent(
                                folder.getParentId(),
                                _ ->
                                        new ArrayList<>()
                        )
                        .add(folder);
            }

            var childrenByParentId =
                    new HashMap<String, List<ResourceFolder>>();

            for (var entry :
                    mutableChildrenByParentId.entrySet())
            {
                entry.getValue().sort(
                        FOLDER_NAME_COMPARATOR
                );

                childrenByParentId.put(
                        entry.getKey(),
                        List.copyOf(
                                entry.getValue()
                        )
                );
            }

            return new SectionIndex(
                    Collections.unmodifiableMap(
                            foldersById
                    ),
                    Collections.unmodifiableMap(
                            childrenByParentId
                    )
            );
        }
    }
}
