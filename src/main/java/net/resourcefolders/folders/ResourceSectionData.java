package net.resourcefolders.folders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResourceSectionData
{
    private List<ResourceFolder> folders = new ArrayList<>();
    private Map<String, String> resourceFolders = new HashMap<>();

    public List<ResourceFolder> getFolders()
    {
        if (folders == null)
        {
            folders = new ArrayList<>();
        }

        return folders;
    }

    public Map<String, String> getResourceFolders()
    {
        if (resourceFolders == null)
        {
            resourceFolders = new HashMap<>();
        }

        return resourceFolders;
    }
}