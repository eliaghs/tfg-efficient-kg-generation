// javac -cp "../stanford-corenlp-4.5.8/*;lib/gson-2.10.1.jar" OpenIEFromConceptsWithVerbs.java

// java -cp ".;../stanford-corenlp-4.5.8/*;lib/gson-2.10.1.jar" OpenIEFromConceptsWithVerbs


import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.util.CoreMap;
import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OpenIEFromConceptsWithVerbs {

    public static void main(String[] args) throws Exception {
        String inputJsonPath = "reference_texts/Homeostasis_concepts_split.json";
        String outputCsvPath = "Homeostasis_chunks_with_verbs_triplets.csv";

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,depparse,coref,natlog,openie");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        StringBuilder result = new StringBuilder();
        result.append("sujeto;predicado;objeto;confianza;frase\n");
        int tripletCount = 0;

        // Leer JSON
        Reader reader = Files.newBufferedReader(Paths.get(inputJsonPath));
        JsonArray data = JsonParser.parseReader(reader).getAsJsonArray();
        reader.close();

        for (JsonElement elem : data) {
            JsonObject entry = elem.getAsJsonObject();
            String fullSentence = entry.get("sentence").getAsString();
            JsonArray conceptsWithVerbs = entry.getAsJsonArray("concepts_with_verbs");

            for (JsonElement conceptElem : conceptsWithVerbs) {
                String chunk = conceptElem.getAsString();

                Annotation doc = new Annotation(chunk);
                pipeline.annotate(doc);

                List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
                for (CoreMap sentence : sentences) {
                    Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

                    for (RelationTriple triple : triples) {
                        String sujeto = triple.subjectLemmaGloss().replace("\"", "\"\"");
                        String predicado = triple.relationLemmaGloss().replace("\"", "\"\"");
                        String objeto = triple.objectLemmaGloss().replace("\"", "\"\"");
                        double confianza = triple.confidence;

                        String row = String.format(Locale.US,
                                "\"%s\";\"%s\";\"%s\";%.3f;\"%s\"",
                                sujeto, predicado, objeto, confianza, fullSentence.replace("\"", "\"\""));

                        result.append(row).append("\n");
                        tripletCount++;
                    }
                }
            }
        }

        Files.write(Paths.get(outputCsvPath), result.toString().getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("✅ Triplets extracted: " + tripletCount);
        System.out.println("📄 Saved to: " + outputCsvPath);
    }
}
