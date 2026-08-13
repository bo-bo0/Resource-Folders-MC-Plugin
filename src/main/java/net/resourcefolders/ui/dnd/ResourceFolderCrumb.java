package net.resourcefolders.ui.dnd;

import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.laf.themes.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class ResourceFolderCrumb
        extends JLabel
{
    private static final int DROP_HIGHLIGHT_TIMEOUT =
            120;

    private final String folderId;

    private final Timer dropHighlightTimer;

    private boolean hovered;
    private boolean dropHighlighted;

    public ResourceFolderCrumb(
            String folderId,
            String name)
    {
        super(
                name,
                UIRES.get("laf.directory"),
                JLabel.LEADING
        );

        this.folderId = folderId;

        setOpaque(false);

        setBorder(
                BorderFactory.createEmptyBorder(
                        2,
                        4,
                        2,
                        4
                )
        );

        setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        dropHighlightTimer =
                new Timer(
                        DROP_HIGHLIGHT_TIMEOUT,
                        _ ->
                        {
                            dropHighlighted = false;

                            updateHighlight();
                        }
                );

        dropHighlightTimer.setRepeats(false);

        addMouseListener(
                new MouseAdapter()
                {
                    @Override
                    public void mouseEntered(
                            MouseEvent event)
                    {
                        hovered = true;

                        updateHighlight();
                    }

                    @Override
                    public void mouseExited(
                            MouseEvent event)
                    {
                        hovered = false;

                        updateHighlight();
                    }
                }
        );
    }

    public String getFolderId()
    {
        return folderId;
    }

    public void pulseDropHighlight()
    {
        dropHighlighted = true;

        updateHighlight();

        dropHighlightTimer.restart();
    }

    public void clearDropHighlight()
    {
        dropHighlightTimer.stop();

        dropHighlighted = false;

        updateHighlight();
    }

    private void updateHighlight()
    {
        boolean highlighted =
                hovered || dropHighlighted;

        setOpaque(highlighted);

        setBackground(
                highlighted
                        ? Theme.current()
                        .getAltBackgroundColor()
                        : Theme.current()
                        .getBackgroundColor()
        );

        repaint();
    }
}