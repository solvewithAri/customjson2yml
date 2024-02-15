package converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class Converter {

    public static void main(String[] args) {
        try {
            convertJsonToYaml("sku.json", "inventory.yaml", "skufieldmap.properties");
            System.out.println("Conversion successful!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void convertJsonToYaml(String jsonFilePath, String yamlFilePath, String mapFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream jsonStream = Converter.class.getResourceAsStream("/" + jsonFilePath);
             InputStream mapStream = Converter.class.getResourceAsStream("/" + mapFilePath)) {

            JsonNode skuJson = objectMapper.readTree(jsonStream);
            Map<String, String> mapping = loadMapping(mapStream);

            List<Map<String, Object>> yamlData = convertJsonToYaml(skuJson, mapping);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            try (Writer writer = new FileWriter(new File(yamlFilePath))) {
                for (Map<String, Object> entry : yamlData) {
                    Map<String, Object> convertedEntry = convertFieldNames(entry, mapping);
                    yaml.dump(Collections.singletonList(convertedEntry), writer);
                }
            }
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

    private static List<Map<String, Object>> convertJsonToYaml(JsonNode skuJson, Map<String, String> mapping) {
        return objectMapper.convertValue(skuJson, List.class);
    }

    private static Map<String, Object> convertFieldNames(Map<String, Object> entry, Map<String, String> mapping) {
        Map<String, Object> convertedEntry = new HashMap<>();
        for (Map.Entry<String, Object> field : entry.entrySet()) {
            String fieldName = field.getKey();
            String yamlFieldName = mapping.getOrDefault(fieldName, fieldName);

            // Exclude fields with the value "NA"
            if (!"NA".equals(yamlFieldName)) {
                convertedEntry.put(yamlFieldName, field.getValue());
            }
        }
        return convertedEntry;
    }

    private static ObjectMapper objectMapper = new ObjectMapper();
}
