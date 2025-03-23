package test

import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Logger

Logger log = LoggerFactory.getLogger(this.class.name);
log.info("Starting ${this.class.name}...")

String
String pass = System.getenv('db_pass')
if (!pass) {
    log.info "No password found in system env, please set it (launch param, or profile), rest of code is likely to fail with missing password to the database"
    pass='pass1234'
}
Map dbConnParams = [url: "jdbc:postgresql://localhost:5432/test", user: 'sean', password: pass, driver: 'org.postgresql.Driver']
def sql = Sql.newInstance(dbConnParams)
//String s = "select * from embeddings"
//def foo = sql.rows(s)
//List<Float> embeddings =
// Create a list of 384 ra
// ndom float values as dummy embeddings
def random = new Random()
def vector = (1..384).collect { random.nextFloat() }

// Convert to PostgreSQL's vector literal format: '[1.1, 2.2, 3.3, ...]'
//def vectorLiteral = vector.join(',')
//log.info("Vector literal: ${vectorLiteral}")

// Connect to DB and insert the record
//def sql = Sql.newInstance(dbUrl, user, password, 'org.postgresql.Driver')

def content = "This is a sample text with an embedding."

sql.executeInsert """
    INSERT INTO embeddings (content, vector)
    VALUES (?, ?::vector)
""", [content, vector.toString()]

println "Record inserted successfully."

log.info("Done!?...")
