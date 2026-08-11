package net.resourcefolders.folders;

import java.util.UUID;

public final class ResourceFolder
{
    private String id;
    private String name;
    private String parentId;

    public ResourceFolder(String name, String parentId)
    {
        this(UUID.randomUUID().toString(), name, parentId);
    }

    public ResourceFolder(String id, String name, String parentId)
    {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getParentId()
    {
        return parentId;
    }

    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }
}