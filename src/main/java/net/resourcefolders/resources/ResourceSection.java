package net.resourcefolders.resources;

import java.awt.*;

public enum ResourceSection
{
    TEXTURES(
            "textures",
            "Textures",
            "WorkspacePanelTextures"
    ),

    MODELS(
            "models",
            "Models",
            "WorkspacePanelModels"
    ),

    ANIMATIONS(
            "animations",
            "Animations",
            "WorkspacePanelAnimations"
    ),

    SOUNDS(
            "sounds",
            "Sounds",
            "WorkspacePanelSounds"
    ),

    STRUCTURES(
            "structures",
            "Structures",
            "WorkspacePanelStructures"
    ),

    SCREENSHOTS(
            "screenshots",
            "Screenshots",
            "WorkspacePanelScreenshots"
    );

    private final String id;
    private final String displayName;
    private final String panelClassName;

    ResourceSection(
            String id,
            String displayName,
            String panelClassName)
    {
        this.id = id;
        this.displayName = displayName;
        this.panelClassName = panelClassName;
    }

    public String getId()
    {
        return id;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public static ResourceSection fromPanel(Component panel)
    {
        var className = panel.getClass().getSimpleName();

        for (var section : values())
        {
            if (section.panelClassName.equals(className))
            {
                return section;
            }
        }

        return null;
    }
}