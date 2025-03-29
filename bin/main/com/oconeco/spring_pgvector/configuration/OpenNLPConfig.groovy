package com.oconeco.spring_pgvector.configuration

import groovy.util.logging.Slf4j
import opennlp.tools.sentdetect.SentenceDetector
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Slf4j
@Configuration
class OpenNLPConfig {
    @Bean
    SentenceDetector sentenceDetector() {
        // Create a SentenceDetector instance
//        def sentenceModel = new SentenceModel('path/to/en-sent.bin')
//        return new SentenceDetectorME(sentenceModel)
        // Load the model file from the classpath
//        def modelIn = new ClassPathResource("en-sent.bin").inputStream
        def modelIn = new ClassPathResource("models/opennlp-en-ud-ewt-sentence-1.2-2.5.0.bin").inputStream
        def model = new SentenceModel(modelIn)
        log.info("Loaded model: ${model}, now about to close it (as per ChatGPT suggestion...?)")
        modelIn.close()

        return new SentenceDetectorME(model)
    }
}
