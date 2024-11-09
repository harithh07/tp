package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.address.model.tag.Tag.MESSAGE_CONSTRAINTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Deletes specified tags from the tag list, but only if they are not in use by any persons.
 */
public class DeleteTagCommand extends UndoableCommand {
    public static final String COMMAND_WORD = "deletetag";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Deletes existing tag(s) from the tag list (case insensitive).\n"
            + MESSAGE_CONSTRAINTS
            + "Parameters: " + PREFIX_TAG + "TAG...\n"
            + "Example: " + COMMAND_WORD + " t/bride's side t/groom's side";

    public static final String MESSAGE_SUCCESS = "Deleted successfully.";
    public static final String MESSAGE_SUCCESS_FORCE = "Force deleted successfully.";

    public static final String MESSAGE_TAGS_IN_USE = "The following tags are in use and cannot be deleted:\n%s\n"
            + "Use 'deletetag -force' to delete tags that are in use.";

    public static final String MESSAGE_ALL_NONEXISTENT = "The following tag(s) has/have not been created before:\n";

    public static final String MESSAGE_SOME_NONEXISTENT = "Existing tag(s) has/have been deleted successfully.\n"
            + "The following tag(s) has/have not been created before:\n";

    private final List<Tag> tags;
    private final Map<Tag, Set<Person>> deletedSet = new HashMap<>();
    private final boolean isForceDelete;

    /**
     * Constructs a DeleteTagCommand to delete the specified tags.
     *
     * @param tags The tags to be deleted.
     */
    public DeleteTagCommand(List<Tag> tags, boolean isForceDelete) {
        requireAllNonNull(tags);
        this.tags = tags;
        this.isForceDelete = isForceDelete;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireAllNonNull(model);

        if (!isForceDelete) {
            checkForTagsInUse(model);
        }

        Set<Tag> tagsSuccessfullyDeleted = handleDelete(model);
        model.removeFilters(tagsSuccessfullyDeleted, new HashSet<>());
        return createCommandResult(tagsSuccessfullyDeleted);
    }

    /**
     * Checks whether the tags to be deleted are currently tagged to guests (in use).
     *
     * @throws CommandException If any of the tags are currently in use.
     */
    private void checkForTagsInUse(Model model) throws CommandException {
        requireAllNonNull(model);
        Set<Tag> tagsInUse = model.getTagsInUse();
        Set<Tag> matchingTags = tagsInUse.stream().filter(tags::contains).collect(Collectors.toSet());

        if (!matchingTags.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_TAGS_IN_USE, matchingTags));
        }
    }

    /**
     * Generates a CommandResult message based on the success or failure of tag deletion.
     * Determines whether to display messages for tags not successfully deleted or tags successfully deleted.
     *
     * @param tagsSuccessfullyDeleted Set of tags that were deleted successfully.
     * @return A CommandResult containing the appropriate success or failure message.
     */
    private CommandResult createCommandResult(Set<Tag> tagsSuccessfullyDeleted) {
        requireAllNonNull(tagsSuccessfullyDeleted);
        List<Tag> tagsNotSuccessfullyDeleted = new ArrayList<>(tags);

        // Get tags that were not deleted successfully
        tagsNotSuccessfullyDeleted.removeAll(tagsSuccessfullyDeleted);

        if (!tagsNotSuccessfullyDeleted.isEmpty() && tagsSuccessfullyDeleted.isEmpty()) {
            return new CommandResult(MESSAGE_ALL_NONEXISTENT + tagsNotSuccessfullyDeleted);
        }

        if (!tagsNotSuccessfullyDeleted.isEmpty()) {
            return new CommandResult(MESSAGE_SOME_NONEXISTENT + tagsNotSuccessfullyDeleted);
        }

        if (isForceDelete) {
            return new CommandResult(MESSAGE_SUCCESS_FORCE);
        }

        return new CommandResult(MESSAGE_SUCCESS);
    }

    /**
     * Deletes tags from the model, and removes each tag from all associated persons.
     *
     * @param model The Model instance containing tags and persons.
     * @return A set of tags that were successfully deleted.
     */
    private Set<Tag> handleDelete(Model model) {
        Set<Tag> tagsSuccessfullyDeleted = model.deleteTags(tags);
        for (Tag tag : tags) {
            model.removeTagFromPersons(tag); // Remove from all persons
        }
        return tagsSuccessfullyDeleted;
    }

    @Override
    public void undo(Model model) {
        requireNonNull(model);
        for (Map.Entry<Tag, Set<Person>> entry : deletedSet.entrySet()) {
            Tag deletedTag = entry.getKey();
            model.addTag(deletedTag);
            for (Person person : entry.getValue()) {
                Set<Tag> newTags = new HashSet<>(person.getTags());
                newTags.add(deletedTag);
                Person updatedPerson = new Person(person.getName(), person.getPhone(), person.getEmail(),
                        person.getRsvpStatus(), newTags);
                model.setPerson(person, updatedPerson);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DeleteTagCommand)) {
            return false;
        }
        DeleteTagCommand otherCommand = (DeleteTagCommand) other;
        return tags.equals(otherCommand.tags)
                && isForceDelete == otherCommand.isForceDelete;
    }
}

