package net.resourcefolders.folders;

import com.google.gson.Gson;
import net.mcreator.workspace.Workspace;

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

    public List<ResourceFolder> getChildren(String parentId)
    {
        return data.getFolders()
                .stream()
                .filter(folder -> Objects.equals(folder.getParentId(), parentId))
                .toList();
    }

    public ResourceFolder getFolder(String id)
    {
        return data.getFolders()
                .stream()
                .filter(folder -> folder.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ResourceFolder createFolder(String name, String parentId)
    {
        var folder = new ResourceFolder(name, parentId);

        data.getFolders().add(folder);

        save();

        return folder;
    }

    public String getResourceFolder(String resourceKey)
    {
        return data.getResourceFolders()
                .getOrDefault(resourceKey, ResourceFolderData.ROOT_ID);
    }

    public void moveResource(String resourceKey, String folderId)
    {
        if (ResourceFolderData.ROOT_ID.equals(folderId))
        {
            data.getResourceFolders().remove(resourceKey);
        }
        else
        {
            data.getResourceFolders().put(resourceKey, folderId);
        }

        save();
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
            var loadedData = GSON.fromJson(json, ResourceFolderData.class);

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
        workspace.putMetadata(METADATA_KEY, GSON.toJson(data));
        workspace.markDirty();
    }
}