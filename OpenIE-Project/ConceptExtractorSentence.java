// javac -cp "C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\stanford-corenlp-4.5.8\*;C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\OpenIE-Project\lib\gson-2.10.1.jar" ConceptExtractorSentence.java

// java -cp "C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\stanford-corenlp-4.5.8\*;C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\OpenIE-Project\lib\gson-2.10.1.jar;." ConceptExtractorSentence "C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\OpenIE-Project\reference_texts\Utilitarianism.txt" > ConceptsPerSentence.json
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.ling.CoreAnnotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;

public class ConceptExtractorSentence {

    public static List<Map<String, Object>> extractConceptsBySentence(String text) {
        StanfordCoreNLP pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,parse",
                        "ssplit.isOneSentence", "false",
                        "tokenize.language", "en"
                )
        );

        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);

        List<Map<String, Object>> sentencesWithConcepts = new ArrayList<>();

        for (CoreSentence sentence : doc.sentences()) {
            Map<String, Object> sentenceData = new LinkedHashMap<>();
            sentenceData.put("sentence", sentence.text());
            
            Tree tree = sentence.constituencyParse();
            Set<String> concepts = new TreeSet<>();
            extractNounPhrases(tree, concepts);
            
            sentenceData.put("concepts", new ArrayList<>(concepts));
            sentencesWithConcepts.add(sentenceData);
        }

        return sentencesWithConcepts;
    }

    private static void extractNounPhrases(Tree tree, Set<String> output) {
        for (Tree subtree : tree) {
            if (subtree.label().value().equals("NP")) {
                String phrase = flattenLeaves(subtree).trim();
                if (!phrase.isEmpty() &&
                    (phrase.split(" ").length > 1 || Character.isUpperCase(phrase.charAt(0)))) {
                    output.add(phrase);
                }
            }
        }
    }

    private static String flattenLeaves(Tree tree) {
        StringBuilder sb = new StringBuilder();
        for (Tree leaf : tree.getLeaves()) {
            sb.append(leaf.value()).append(" ");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Por favor, indica el archivo de entrada (.txt).");
            System.exit(1);
        }

        try {
            // Lee el texto desde el archivo indicado
            String texto = Files.readString(Paths.get(args[0]));
            List<Map<String, Object>> sentencesWithConcepts = extractConceptsBySentence(texto);

            // Imprime en JSON
            Gson gson = new Gson();
            System.out.println(gson.toJson(sentencesWithConcepts));

        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
    }
}