package net.resourcefolders.folders;

import java.util.HashMap;
import java.util.Map;

public final class ResourceFolderData
{
    public static final String ROOT_ID = "root";

    private Map<String, ResourceSectionData> sections = new HashMap<>();

    public Map<String, ResourceSectionData> getSections()
    {
        if (sections == null)
        {
            sections = new HashMap<>();
        }

        return sections;
    }
}