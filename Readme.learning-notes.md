# Learning and Reminder Notes

## Learning Notes
### Spring Boot
#### Annotations
* ```    /**
     * PostgreSQL tsvector for full-text search.
     * This field is managed by a database trigger and should not be modified directly.
     */
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector```

### Groovy and Spring Boot
* Controller params need explicit `name` attribute (due to reflection differences between groovy and java)

### Postgres full text search
#### ChatGPT
In short:

    plainto_tsquery:
        Interprets the entire input string as plain text.
        Automatically strips out punctuation and special full‑text operators.
        Splits the input into tokens and combines them with a logical AND.
        Example:

    SELECT plainto_tsquery('english', 'dog barking');
    /* yields something like: 'dog' & 'bark' */

to_tsquery:

    Expects a text search query with possible operators (&, |, !, etc.).
    Allows complex boolean expressions, prefix matches, or phrase searches.
    If your input has no operators, you must insert them yourself or it will raise an error.
    Example:

        SELECT to_tsquery('english', 'dog & bark');
        /* yields: 'dog' & 'bark' */

Practical difference

    If you have raw user input and just want a simple “search by these words,” plainto_tsquery is simpler and safer. It will transform the user’s text into a search query that’s logically AND-ed by default, ignoring anything that looks like an operator.
    If you want to let users build advanced queries (e.g., dog & (bark | growl)), to_tsquery is what you use to respect those operators. However, you must sanitize the input properly to avoid errors or security issues.

## Reminders
