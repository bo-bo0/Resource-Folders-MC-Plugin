package net.resourcefolders.ui;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class FolderFilteredListModel<T>
        extends AbstractListModel<T>
        implements ListDataListener
{
    private final ListModel<T> sourceModel;
    private final Predicate<T> filter;

    private final List<T> visibleItems =
            new ArrayList<>();

    public FolderFilteredListModel(
            ListModel<T> sourceModel,
            Predicate<T> filter)
    {
        this.sourceModel = sourceModel;
        this.filter = filter;

        sourceModel.addListDataListener(this);

        refresh();
    }

    @Override
    public int getSize()
    {
        return visibleItems.size();
    }

    @Override
    public T getElementAt(int index)
    {
        return visibleItems.get(index);
    }

    public void refresh()
    {
        var oldSize = visibleItems.size();

        visibleItems.clear();

        for (int i = 0; i < sourceModel.getSize(); i++)
        {
            var item = sourceModel.getElementAt(i);

            if (item != null && filter.test(item))
            {
                visibleItems.add(item);
            }
        }

        var newSize = visibleItems.size();

        if (oldSize > 0)
        {
            fireIntervalRemoved(
                    this,
                    0,
                    oldSize - 1
            );
        }

        if (newSize > 0)
        {
            fireIntervalAdded(
                    this,
                    0,
                    newSize - 1
            );
        }
    }

    @Override
    public void intervalAdded(ListDataEvent event)
    {
        refresh();
    }

    @Override
    public void intervalRemoved(ListDataEvent event)
    {
        refresh();
    }

    @Override
    public void contentsChanged(ListDataEvent event)
    {
        refresh();
    }
}