package net.resourcefolders.folders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResourceFolderData
{
    public static final String ROOT_ID = "root";

    private final List<ResourceFolder> folders = new ArrayList<>();
    private final Map<String, String> resourceFolders = new HashMap<>();

    public List<ResourceFolder> getFolders()
    {
        return folders;
    }

    public Map<String, String> getResourceFolders()
    {
        return resourceFolders;
    }
}