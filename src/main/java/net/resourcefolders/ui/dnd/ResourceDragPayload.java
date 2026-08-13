package net.resourcefolders.ui.dnd;

import java.util.List;

public final class ResourceDragPayload
{
    private final String sectionId;
    private final List<String> resourceKeys;

    public ResourceDragPayload(
            String sectionId,
            List<String> resourceKeys)
    {
        this.sectionId = sectionId;

        this.resourceKeys =
                List.copyOf(resourceKeys);
    }

    public String getSectionId()
    {
        return sectionId;
    }

    public List<String> getResourceKeys()
    {
        return resourceKeys;
    }
}