// javac -cp "../stanford-corenlp-4.5.8/*" OpenIEEnhanced.java
// java -cp ".;../stanford-corenlp-4.5.8/*" OpenIEEnhanced

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class OpenIEEnhanced {
    public static void main(String[] args) throws Exception {
        String inputFile = "reference_texts\\ut2.txt";
        String outputFile = "ut2_enriched.csv";

        String text = readFileAsString(inputFile);

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,entitymentions,parse,depparse,coref,natlog,openie,relation");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);

        Map<IntPair, String> mentionToRepresentative = new HashMap<>();
        Map<Integer, CorefChain> corefChains = doc.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        for (CorefChain chain : corefChains.values()) {
            CorefMention rep = chain.getRepresentativeMention();
            String repText = getMentionText(doc, rep);

            for (CorefMention mention : chain.getMentionsInTextualOrder()) {
                if (mention == rep) continue;
                IntPair key = new IntPair(mention.sentNum, mention.startIndex);
                mentionToRepresentative.put(key, repText);
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("sujeto;predicado;objeto;confianza;tipo_relacion;tipo_sujeto;tipo_objeto;frase\n");
        int tripletCount = 0;

        List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            String originalSentence = sentence.toString().replace("\"", "\"\"");
            int sentNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class) + 1;

            // OpenIE triplets
            Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
            for (RelationTriple triple : triples) {
                String sujeto = resolveMention(sentence, triple.subjectTokenSpan(), mentionToRepresentative, sentNum);
                String objeto = resolveMention(sentence, triple.objectTokenSpan(), mentionToRepresentative, sentNum);
                String predicado = triple.relationLemmaGloss().replace("\"", "\"\"");
                double confianza = triple.confidence;

                String tipoSujeto = getEntityType(sentence, triple.subjectTokenSpan());
                String tipoObjeto = getEntityType(sentence, triple.objectTokenSpan());

                result.append(String.format(Locale.US,
                        "\"%s\";\"%s\";\"%s\";%.3f;\"%s\";\"%s\";\"%s\";\"%s\"\n",
                        sujeto, predicado, objeto, confianza, "openie", tipoSujeto, tipoObjeto, originalSentence));
                tripletCount++;
            }

            // Relation extractor
            List<RelationMention> relations = sentence.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
            for (RelationMention rel : relations) {
                String sujeto = rel.getEntityMentionArgs().get(0).getExtentString().replace("\"", "\"\"");
                String objeto = rel.getEntityMentionArgs().get(1).getExtentString().replace("\"", "\"\"");
                String predicado = rel.getType().replace("\"", "\"\"");
                result.append(String.format(Locale.US,
                        "\"%s\";\"%s\";\"%s\";1.000;\"%s\";;;\"%s\"\n",
                        sujeto, predicado, objeto, "relation", originalSentence));
                tripletCount++;
            }
        }

        writeToFile(outputFile, result.toString());
        System.out.println("Total de tripletas generadas: " + tripletCount);
        System.out.println("Tripletas guardadas en: " + outputFile);
    }

    public static String resolveMention(CoreMap sentence, Pair<Integer, Integer> span, Map<IntPair, String> corefMap, int sentNum) {
        IntPair key = new IntPair(sentNum, span.first() + 1);
        if (corefMap.containsKey(key)) {
            return corefMap.get(key).replace("\"", "\"\"");
        } else {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            StringBuilder sb = new StringBuilder();
            for (int i = span.first(); i < span.second(); i++) {
                sb.append(tokens.get(i).originalText()).append(" ");
            }
            return sb.toString().trim().replace("\"", "\"\"");
        }
    }

    public static String getEntityType(CoreMap sentence, Pair<Integer, Integer> span) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        Set<String> types = new HashSet<>();
        for (int i = span.first(); i < span.second() && i < tokens.size(); i++) {
            String ner = tokens.get(i).ner();
            if (ner != null && !ner.equals("O")) {
                types.add(ner);
            }
        }
        return String.join(",", types).replace("\"", "\"\"");
    }

    public static String readFileAsString(String filePath) throws IOException {
        return String.join(" ", Files.readAllLines(Paths.get(filePath)));
    }

    public static void writeToFile(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static String getMentionText(Annotation doc, CorefMention mention) {
        CoreMap sentence = doc.get(CoreAnnotations.SentencesAnnotation.class).get(mention.sentNum - 1);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        StringBuilder sb = new StringBuilder();
        for (int i = mention.startIndex - 1; i < mention.endIndex - 1 && i < tokens.size(); i++) {
            sb.append(tokens.get(i).originalText()).append(" ");
        }
        return sb.toString().trim();
    }
}
