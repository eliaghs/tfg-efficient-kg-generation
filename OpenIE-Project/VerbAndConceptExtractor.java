// javac -cp "C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\stanford-corenlp-4.5.8\*;C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\OpenIE-Project\lib\gson-2.10.1.jar" VerbAndConceptExtractor.java

// java -cp "C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\stanford-corenlp-4.5.8\*;C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\OpenIE-Project\lib\gson-2.10.1.jar;." VerbAndConceptExtractor "C:\Users\eliag\OneDrive - Universidad Politécnica de Madrid\TFG\openie pruebas 4-3\openie pruebas 4-3\stanford-corenlp\OpenIE-Project\reference_texts\Utilitarianism.txt" > VerbsAndConcepts.json

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;

public class VerbAndConceptExtractor {

    public static List<Map<String, Object>> extractVerbsAndConceptsBySentence(String text) {
        StanfordCoreNLP pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,parse",
                        "ssplit.isOneSentence", "false",
                        "tokenize.language", "en"
                )
        );

        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);

        List<Map<String, Object>> sentencesData = new ArrayList<>();

        for (CoreSentence sentence : doc.sentences()) {
            Map<String, Object> sentenceInfo = new LinkedHashMap<>();
            sentenceInfo.put("sentence", sentence.text());

            // Extraer conceptos (sintagmas nominales)
            Tree tree = sentence.constituencyParse();
            Set<String> concepts = new TreeSet<>();
            extractNounPhrases(tree, concepts);
            sentenceInfo.put("concepts", new ArrayList<>(concepts));

            // Extraer verbos (por POS tags)
            Set<String> verbs = new TreeSet<>();
            extractVerbs(sentence, verbs);
            sentenceInfo.put("verbs", new ArrayList<>(verbs));

            sentencesData.add(sentenceInfo);
        }

        return sentencesData;
    }

    private static void extractNounPhrases(Tree tree, Set<String> output) {
        for (Tree subtree : tree) {
            if (subtree.label().value().equals("NP")) {
                String phrase = flattenLeaves(subtree).trim();
                // if (!phrase.isEmpty() &&
                //     (phrase.split(" ").length > 1 || Character.isUpperCase(phrase.charAt(0)))) {
                //     output.add(phrase);
                // }
                if (!phrase.isEmpty()) {
                    output.add(phrase);
                }
            }
        }
    }

    private static void extractVerbs(CoreSentence sentence, Set<String> output) {
        for (CoreLabel token : sentence.tokens()) {
            String pos = token.get(PartOfSpeechAnnotation.class);
            if (pos.startsWith("VB")) {
                output.add(token.word());
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
            String texto = Files.readString(Paths.get(args[0]));
            List<Map<String, Object>> result = extractVerbsAndConceptsBySentence(texto);

            Gson gson = new Gson();
            System.out.println(gson.toJson(result));

        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
    }
}


