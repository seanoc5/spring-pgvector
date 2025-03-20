# Spring PGVector Semantic Search

A semantic search application built with Spring Boot, Spring AI, and pgvector to explore and compare different embedding models and vector search approaches.

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
I have tried configuring docker to ease config,test,debug process for solr configuration. I am still learning docker/docker-compose, so I am open to suggestions and feedback. 

My goal is to just reload the solr image, and the the solr-init.sh script will run again. That should naively _(dangerously?)_ re-upload the solr `contracts`configset to the container.  
**NOTE**: This will overwrite the existing `contracts` configset.

This is just a demo/learning app, so unlikely to have any "production" concerns, but do consider switching from docker to "real" services. 
`Ollama` is tough to get using a local GPU, and I have not tried for this demo docker setup. But if you have a local GPU, it should be easy to comment out the docker-compose `ollama` service and use the local `ollama` service.

### Intellij
In the `services` tool, I found I can stop/start the solr container and it will be "reloaded". The image will not be rebuilt, but the container will be reloaded. This **should** reload the solr `contracts` configset as well as re `post` the sample csv data. It is a bit hacky, so any suggestions on a better way to do this are welcome.


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
