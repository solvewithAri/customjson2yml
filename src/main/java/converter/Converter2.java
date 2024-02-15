package converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class Converter2 {

    public static void main(String[] args) {
        try {
            convertJsonToYaml("sku.json", "inventory3.yaml", "skufieldmap.properties");
            System.out.println("Conversion successful!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void convertJsonToYaml(String jsonFilePath, String yamlFilePath, String mapFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream jsonStream = Converter2.class.getResourceAsStream("/" + jsonFilePath);
             InputStream mapStream = Converter2.class.getResourceAsStream("/" + mapFilePath);
             Writer writer = new FileWriter(new File(yamlFilePath))) {

            JsonNode skuJson = objectMapper.readTree(jsonStream);
            Map<String, String> mapping = loadMapping(mapStream);

            writeCustomYaml(writer, skuJson, mapping);
        }
    }

    private static Map<String, String> loadMapping(InputStream mapStream) throws IOException {
        Properties properties = new Properties();
        properties.load(mapStream);

        Map<String, String> mapping = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            mapping.put(key, properties.getProperty(key));
        }
        return mapping;
    }

    private static void writeCustomYaml(Writer writer, JsonNode skuJson, Map<String, String> mapping) throws IOException {
        StringBuilder yamlOutput = new StringBuilder("skus:\n");
        for (JsonNode sku : skuJson) {
            yamlOutput.append("  - {");
            boolean firstField = true;
            for (Iterator<Map.Entry<String, JsonNode>> fields = sku.fields(); fields.hasNext(); ) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                String yamlFieldName = mapping.getOrDefault(fieldName, fieldName);

                // Exclude fields with the value "NA"
                if (!"NA".equals(yamlFieldName)) {
                    if (!firstField) {
                        yamlOutput.append(", ");
                    }
                    yamlOutput.append("\"").append(yamlFieldName).append("\": ");
                    yamlOutput.append(convertField(field.getValue()));
                    firstField = false;
                }
            }
            yamlOutput.append("}\n");
        }
        writer.write(yamlOutput.toString());
    }

    private static String convertField(JsonNode field) {
        if (field.isTextual()) {
            return "\"" + field.textValue() + "\"";
        } else if (field.isBoolean()) {
            return String.valueOf(field.booleanValue());
        } else if (field.isNumber()) {
            return String.valueOf(field.numberValue());
        } else if (field.isArray()) {
            StringBuilder arrayOutput = new StringBuilder("[");
            boolean firstElement = true;
            for (JsonNode element : field) {
                if (!firstElement) {
                    arrayOutput.append(", ");
                }
                arrayOutput.append(convertField(element));
                firstElement = false;
            }
            arrayOutput.append("]");
            return arrayOutput.toString();
        } else if (field.isObject()) {
            StringBuilder objectOutput = new StringBuilder("{");
            boolean firstEntry = true;
            for (Iterator<Map.Entry<String, JsonNode>> fields = field.fields(); fields.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (!firstEntry) {
                    objectOutput.append(", ");
                }
                objectOutput.append("\"").append(entry.getKey()).append("\": ");
                objectOutput.append(convertField(entry.getValue()));
                firstEntry = false;
            }
            objectOutput.append("}");
            return objectOutput.toString();
        }
        return null;
    }
}