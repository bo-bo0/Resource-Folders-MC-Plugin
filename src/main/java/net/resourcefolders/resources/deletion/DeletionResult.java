package net.resourcefolders.resources.deletion;

import java.util.List;

public record DeletionResult(
        boolean successful,
        List<String> messages)
{
    public DeletionResult
    {
        messages =
                List.copyOf(messages);
    }

    public static DeletionResult success()
    {
        return success(
                List.of()
        );
    }

    public static DeletionResult success(
            List<String> messages)
    {
        return new DeletionResult(
                true,
                messages
        );
    }

    public static DeletionResult failure(
            List<String> messages)
    {
        return new DeletionResult(
                false,
                messages
        );
    }
}
