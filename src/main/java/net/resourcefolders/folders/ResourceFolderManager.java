package net.resourcefolders.folders;

import com.google.gson.Gson;
import net.mcreator.workspace.Workspace;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ResourceFolderManager
{
    private static final String METADATA_KEY = "resource_folders";
    private static final Gson GSON = new Gson();

    private final Workspace workspace;

    private ResourceFolderData data;

    public ResourceFolderManager(Workspace workspace)
    {
        this.workspace = workspace;
        this.data = load();
    }

    public ResourceFolderData getData()
    {
        return data;
    }

    public List<ResourceFolder> getChildren(
            String sectionId,
            String parentId)
    {
        return getSection(sectionId)
                .getFolders()
                .stream()
                .filter(folder ->
                        Objects.equals(folder.getParentId(), parentId))
                .sorted((a, b) ->
                        a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    public ResourceFolder getFolder(
            String sectionId,
            String folderId)
    {
        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            return null;
        }

        return getSection(sectionId)
                .getFolders()
                .stream()
                .filter(folder -> folder.getId().equals(folderId))
                .findFirst()
                .orElse(null);
    }

    public ResourceFolder createFolder(
            String sectionId,
            String name,
            String parentId)
    {
        var folder = new ResourceFolder(name, parentId);

        getSection(sectionId)
                .getFolders()
                .add(folder);

        save();

        return folder;
    }

    public boolean folderExists(
            String sectionId,
            String name,
            String parentId)
    {
        return getChildren(sectionId, parentId)
                .stream()
                .anyMatch(folder ->
                        folder.getName().equalsIgnoreCase(name));
    }

    public String getParentId(
            String sectionId,
            String folderId)
    {
        var folder = getFolder(sectionId, folderId);

        if (folder == null || folder.getParentId() == null)
        {
            return ResourceFolderData.ROOT_ID;
        }

        return folder.getParentId();
    }

    public String getPath(
            String sectionId,
            String rootName,
            String folderId)
    {
        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            return rootName;
        }

        var parts = new ArrayDeque<String>();
        var visitedFolders = new HashSet<String>();
        var currentId = folderId;

        while (!ResourceFolderData.ROOT_ID.equals(currentId))
        {
            if (!visitedFolders.add(currentId))
            {
                break;
            }

            var folder = getFolder(sectionId, currentId);

            if (folder == null)
            {
                break;
            }

            parts.addFirst(folder.getName());

            currentId = folder.getParentId();

            if (currentId == null)
            {
                break;
            }
        }

        if (parts.isEmpty())
        {
            return rootName;
        }

        return rootName + " / " + String.join(" / ", parts);
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

    public void moveResource(
            String sectionId,
            String resourceKey,
            String folderId)
    {
        var resourceFolders =
                getSection(sectionId).getResourceFolders();

        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            resourceFolders.remove(resourceKey);
        }
        else
        {
            resourceFolders.put(resourceKey, folderId);
        }

        save();
    }

    private ResourceSectionData getSection(String sectionId)
    {
        return data.getSections()
                .computeIfAbsent(
                        sectionId,
                        _ -> new ResourceSectionData()
                );
    }

    private ResourceFolderData load()
    {
        var storedData = workspace.getMetadata(METADATA_KEY);

        if (!(storedData instanceof String json) || json.isBlank())
        {
            return new ResourceFolderData();
        }

        try
        {
            var loadedData =
                    GSON.fromJson(json, ResourceFolderData.class);

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
}