package net.resourcefolders.ui;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        refresh(false);
    }

    private void refresh(
            boolean notifyUnchangedContents)
    {
        var newVisibleItems =
                new ArrayList<T>();

        for (int i = 0; i < sourceModel.getSize(); i++)
        {
            var item = sourceModel.getElementAt(i);

            if (item != null && filter.test(item))
            {
                newVisibleItems.add(item);
            }
        }

        var oldSize = visibleItems.size();
        var newSize = newVisibleItems.size();

        int commonPrefixLength = 0;
        int maximumCommonLength =
                Math.min(
                        oldSize,
                        newSize
                );

        while (commonPrefixLength
                < maximumCommonLength
                && visibleItems.get(
                        commonPrefixLength)
                == newVisibleItems.get(
                        commonPrefixLength))
        {
            commonPrefixLength++;
        }

        if (oldSize == newSize
                && commonPrefixLength == oldSize)
        {
            if (notifyUnchangedContents
                    && oldSize > 0)
            {
                fireContentsChanged(
                        this,
                        0,
                        oldSize - 1
                );
            }

            return;
        }

        int commonSuffixLength = 0;

        while (commonSuffixLength
                < maximumCommonLength
                - commonPrefixLength
                && visibleItems.get(
                        oldSize
                                - commonSuffixLength
                                - 1)
                == newVisibleItems.get(
                        newSize
                                - commonSuffixLength
                                - 1))
        {
            commonSuffixLength++;
        }

        int removedCount =
                oldSize
                        - commonPrefixLength
                        - commonSuffixLength;

        int addedCount =
                newSize
                        - commonPrefixLength
                        - commonSuffixLength;

        boolean replacementsEquivalent =
                removedCount == addedCount;

        for (int i = 0;
             replacementsEquivalent
                     && i < addedCount;
             i++)
        {
            replacementsEquivalent =
                    Objects.equals(
                            visibleItems.get(
                                    commonPrefixLength + i),
                            newVisibleItems.get(
                                    commonPrefixLength + i
                            )
                    );
        }

        if (replacementsEquivalent)
        {
            for (int i = 0;
                 i < addedCount;
                 i++)
            {
                visibleItems.set(
                        commonPrefixLength + i,
                        newVisibleItems.get(
                                commonPrefixLength + i
                        )
                );
            }

            fireContentsChanged(
                    this,
                    commonPrefixLength,
                    commonPrefixLength
                            + addedCount - 1
            );

            return;
        }

        if (removedCount > 0)
        {
            visibleItems
                    .subList(
                            commonPrefixLength,
                            commonPrefixLength
                                    + removedCount
                    )
                    .clear();

            fireIntervalRemoved(
                    this,
                    commonPrefixLength,
                    commonPrefixLength
                            + removedCount - 1
            );
        }

        if (addedCount > 0)
        {
            visibleItems.addAll(
                    commonPrefixLength,
                    newVisibleItems.subList(
                            commonPrefixLength,
                            commonPrefixLength
                                    + addedCount
                    )
            );

            fireIntervalAdded(
                    this,
                    commonPrefixLength,
                    commonPrefixLength
                            + addedCount - 1
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
        refresh(true);
    }
}
