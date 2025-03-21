# Spring PGVector Semantic Search

A semantic search application built with Spring Boot, Spring AI, and pgvector to explore and compare different embedding models and vector search approaches.

## Quick Start

1. Clone the repository: `git clone https://github.com/seanoc5/spring-pgvector/`
2. Navigate to the project directory: `cd spring-pgvector`
3. Run `./gradlew bootRun` to start the application

Docker Compose will start PostgreSQL, Ollama, Solr, and Zookeeper for you.  
A solr-init.sh script will load sample data into the Solr collection (sanity-check) for demo/debug of solr config _(not directly related to this project)_.  
Ollama will start on port 11434 for embedding model hosting (most likely without GPU acceleration). Comment out the `ollama` service in `docker-compose.yaml` if you want to use local embeddings.  Similarly, and of the 4 sould be fine running 'natively' rather than in docker.
Spring AI and pgvector (via starters in build.gradle) will configure a single shared table for semantic search. See application.yaml for model configuration.   
Future  efforts will likely add JPA based custom vector fields.  
AFAIU: the vectors are converted to a csv string and sent to postgres. Postgres then (re)splits that string in to individual vectors. This might be a touch inefficent, but it seems like it is common practice (??), and I will wait until someone suggests a better way. 


Then navigate to http://localhost:8080/embedding/create in your browser to access the demo content upload form. The backend embedding controller and service will split the text content into paragraphs and sentences for embedding generation and storage. 

You should be redirected to http://localhost:8080/embedding/list, where you can see the (hacky) list of Spring AI Documents. You can enter a search as you type in the query and see the results. 

## Project Overview

This project was created as a learning journey to explore:
- Spring Boot application development
- Spring AI integration for embeddings generation
- pgvector for efficient vector storage and similarity search
- Docker and Docker Compose for containerization
- Solr integration for text search capabilities

The application provides a platform for semantic search with configurable embedding models, distance metrics, and chunking strategies, laying the groundwork for side-by-side comparison of different approaches.

## Features

- **Semantic Search**: Search documents using natural language queries
- **Configurable Embedding Models**: Support for various embedding models (nomic-embed-text, gte-large, etc.)
- **Vector Storage**: Efficient storage and retrieval of embeddings using pgvector
- **Text Chunking**: Customizable document chunking strategies using paragraph and sentence splitting
- **Docker Integration**: Containerized setup for easy deployment and testing
- **Solr Integration**: Additional text search capabilities with Solr

## Technology Stack

- **Backend**: Spring Boot 3.4.3 with Groovy 4.0.24
- **AI/ML**: Spring AI for embeddings generation
- **Database**: PostgreSQL with pgvector extension
- **Vector Models**: Ollama for local embedding model hosting
- **Search**: Solr for text search capabilities
- **Frontend**: Thymeleaf and HTMX for dynamic UI updates
- **Security**: Spring Security for basic authentication
- **Containerization**: Docker and Docker Compose

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21 or higher
- Gradle

### Running the Application

1. Clone the repository
2. Start the required services using Docker Compose:

```bash
docker compose up -d
```

This will start:
- PostgreSQL with pgvector extension
- Ollama for embedding models
- Solr and Zookeeper for text search

3. Run the Spring Boot application:

```bash
./gradlew bootRun
```

4. Access the application at http://localhost:8080

## Configuration

The application is highly configurable through the `application.yaml` file:

### Embedding Models

```yaml
spring:
  ai:
    ollama:
      embedding:
        options:
          model: nomic-embed-text     # 768 dimensions
          # Other options:
          # model: gte-large
          # model: nomic-ai/nomic-embed-text-v1.5
          # model: Snowflake/snowflake-arctic-embed-m-v2.0
```

### Vector Store Configuration

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        dimensions: 768
        index-type: hnsw
        schema-validation: true
        initialize-schema: true
```

### Solr Configuration

```yaml
solr:
  host: http://localhost:8983/solr
  collection: contracting
  port: 8983
```

## Usage and Development notes
### Docker
I have tried to configuring docker to ease config,test,debug process for solr configuration. 
I am still learning docker/docker-compose, so I am open to suggestions and feedback.
Note: solr depends on zookeeper, so I have been able to just start solr, and docker-compose will start zookeeper.

My goal is to just reload the solr image, and the the solr-init.sh script will run again. That should naively _(dangerously?)_ re-upload the solr `sanity-check`configset to the container.  
**NOTE**: This will overwrite the existing `sanity-check` configset.


#### Ollama
This is just a demo/learning app, so unlikely to have any "production" concerns, but do consider switching from docker to "real" services. 
`Ollama` is tough to get using a local GPU, and I have not tried for this demo docker setup. But if you have a local GPU, it should be easy to comment out the docker-compose `ollama` service and use the local `ollama` service.

#### Solr
I have had decent luck:
* modifying the `sanity-check` solr configset and then 
* `alt-8` in intellij to get to services tool window, then 
* ctrl-f2 to stop the solr container, then 
* ctrl-enter to re-start the solr container.

There are likely 'smarter' ways to do this, but so far this works.

**Note:** remember that zookeeper keeps the confiration for solr. 
So if you change the solr configset, you need to re-upload it to zookeeper. 
If things get hinky, you may want to delete/repopulate the zookeeper volumes (or just do it the **proper** way with zk commands, but that takes thought and effort...).

### Intellij
In the `services` tool, I found I can stop/start the solr container and it will be "reloaded". 
The image will not be rebuilt, but the container will be reloaded. 
This **should** reload the solr `sanity-check` configset as well as re `post` the sample csv data. 
It is a bit hacky, so any suggestions on a better way to do this are welcome.


## Searching
### Solr
**Note:** `solr-init.sh` loads a sample csv file into the `sanity-check` collection for demo/debug. 
The intent is to be able to check the default configs, including field types. 

The `sanity-check` collection has the following fields:
* text_gen
* text_en
* shingle (custom field type)
* sayt (custom field type)
* bucket_tight (custom field type) -- title and description copied to a "tight" field
* bucket_fuzzy (custom field type) -- title and description copied to a "fuzzy" field

There is some minor solrschema configuration in the `sanity-check` configset:
* synonyms
* protected words
* highlighting (`hl.*`)
* faceting (`facet.*`)
* fields to return (`fl`) 
* query fields (edismax with boosting, `qf`)
* phrase fields (edismax with boosting, `pf`)

This provides a way to compare keyword search (solr) against dense vector embeddings (pgvector).

### PGVector
This provides a way to compare dense vector embeddings (pgvector) against keyword search (solr).
`application.yaml` can/should be used to configure the embedding models and vector store.

### Coming Soon:
- UI for comparing search results
- ability to compare different embedding models, distance metrics, and chunking strategies (well... perhaps not **soon**...)

## Future Development

The next phase of development will focus on:

1. Side-by-side comparison functionality for:
   - Different embedding models (nomic, gte, mxbai, etc.)
   - Various distance metrics (COSINE_DISTANCE, EUCLIDEAN_DISTANCE, etc.)
   - Different chunking strategies

2. Performance metrics and visualization
3. Enhanced UI for comparing search results
4. Support for more document formats and sources

## Contributing

Contributions are welcome! Feel free to submit pull requests or open issues to suggest improvements or report bugs.

## License

This project is open source and available under the [MIT License](LICENSE).
