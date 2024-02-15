package converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

class Converter3 {

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

        try (InputStream jsonStream = Converter3.class.getResourceAsStream("/" + jsonFilePath);
             InputStream mapStream = Converter3.class.getResourceAsStream("/" + mapFilePath);
             Writer writer = new FileWriter(new File(yamlFilePath))) {

            JsonNode skuJson = objectMapper.readTree(jsonStream);
            Map<String, String> mapping = loadMapping(mapStream);

            writeCustomYaml(writer, skuJson, mapping);
        }
    }

    private static Map<String, String> loadMapping(InputStream mapStream) throws IOException {
        Properties properties = new Properties();
        properties.load(mapStream);

        Map<String, String> mapping = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            mapping.put(key, properties.getProperty(key));
        }
        return mapping;
    }

    private static void writeCustomYaml(Writer writer, JsonNode skuJson, Map<String, String> mapping) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        List<String> yamlOutput = new ArrayList<>();
        for (JsonNode sku : skuJson) {
            Map<String, Object> convertedEntry = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> fields = sku.fields(); fields.hasNext(); ) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                String yamlFieldName = mapping.getOrDefault(fieldName, fieldName);

                // Exclude fields with the value "NA"
                if (!"NA".equals(yamlFieldName)) {
                    convertedEntry.put(yamlFieldName, convertField(field.getValue()));
                }
            }
            yamlOutput.add(yaml.dump(convertedEntry));
        }

        writer.write("skus:\n");
        for (String output : yamlOutput) {
            writer.write("  - " + output);
        }
    }

    private static Object convertField(JsonNode field) {
        if (field.isTextual()) {
            return field.textValue();
        } else if (field.isBoolean()) {
            return field.booleanValue();
        } else if (field.isNumber()) {
            return field.numberValue();
        } else if (field.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode element : field) {
                list.add(convertField(element));
            }
            return list;
        } else if (field.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> fields = field.fields(); fields.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put(entry.getKey(), convertField(entry.getValue()));
            }
            return map;
        }
        return null;
    }
}
