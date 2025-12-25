package com.cogarc.notification.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Service
public class ProducerService {

    private static final Logger logger = LoggerFactory.getLogger(ProducerService.class);

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.samples.directories.order-status}")
    private String orderStatusDir;

    @Value("${app.samples.directories.ucc}")
    private String uccDir;

    @Value("${app.samples.directories.tour-appointment}")
    private String tourAppointmentDir;

    @Value("${app.samples.directories.truckload}")
    private String truckloadDir;

    @Value("${app.pubsub.topics.order-status}")
    private String orderStatusTopic;

    @Value("${app.pubsub.topics.ucc}")
    private String uccTopic;

    @Value("${app.pubsub.topics.tour-appointment}")
    private String tourAppointmentTopic;

    @Value("${app.pubsub.topics.truckload}")
    private String truckloadTopic;

    private final Map<String, Schema> schemas = new HashMap<>();

    public ProducerService() {
        // Load Avro schemas
        try {
            schemas.put("order-status", new Schema.Parser().parse(
                getClass().getResourceAsStream("/avro/OrderStatus.avsc")));
            schemas.put("ucc", new Schema.Parser().parse(
                getClass().getResourceAsStream("/avro/UCC.avsc")));
            schemas.put("tour-appointment", new Schema.Parser().parse(
                getClass().getResourceAsStream("/avro/TourAppointmentConfirmation.avsc")));
            schemas.put("truckload", new Schema.Parser().parse(
                getClass().getResourceAsStream("/avro/TruckloadConfirmation.avsc")));
        } catch (IOException e) {
            logger.error("Failed to load Avro schemas", e);
            throw new RuntimeException("Failed to load Avro schemas", e);
        }
    }

    public int publishBatch(String messageType) {
        String directory = getDirectoryForType(messageType);
        String topic = getTopicForType(messageType);
        Schema schema = schemas.get(messageType);

        if (directory == null || topic == null || schema == null) {
            logger.error("Invalid message type: {}", messageType);
            return 0;
        }

        int publishedCount = 0;
        try {
            // Try to load from classpath first (for resources in src/main/resources)
            List<String> jsonFileNames = findJsonFilesInClasspath(directory);
            
            // If not found in classpath, try file system (relative to working directory)
            if (jsonFileNames.isEmpty()) {
                logger.info("No files found in classpath, trying file system path: {}", directory);
                Path dirPath = Paths.get(directory);
                if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                    File[] jsonFiles = dirPath.toFile().listFiles((dir, name) -> name.endsWith(".json"));
                    if (jsonFiles != null && jsonFiles.length > 0) {
                        jsonFileNames = Arrays.stream(jsonFiles)
                            .map(File::getName)
                            .collect(Collectors.toList());
                        logger.info("Found {} JSON files in file system: {}", jsonFileNames.size(), dirPath.toAbsolutePath());
                    } else {
                        logger.warn("No JSON files found in file system directory: {}", dirPath.toAbsolutePath());
                    }
                } else {
                    logger.warn("File system directory does not exist: {}", dirPath.toAbsolutePath());
                    // Try target/classes as fallback
                    Path targetPath = Paths.get("target/classes", directory);
                    if (Files.exists(targetPath) && Files.isDirectory(targetPath)) {
                        File[] jsonFiles = targetPath.toFile().listFiles((dir, name) -> name.endsWith(".json"));
                        if (jsonFiles != null && jsonFiles.length > 0) {
                            jsonFileNames = Arrays.stream(jsonFiles)
                                .map(File::getName)
                                .collect(Collectors.toList());
                            logger.info("Found {} JSON files in target/classes: {}", jsonFileNames.size(), targetPath.toAbsolutePath());
                        }
                    }
                }
            }

            if (jsonFileNames.isEmpty()) {
                logger.warn("No JSON files found in directory: {}", directory);
                return 0;
            }

            logger.info("Found {} JSON files in {}", jsonFileNames.size(), directory);

            for (String jsonFileName : jsonFileNames) {
                try {
                    String jsonContent = loadFileContent(directory, jsonFileName);
                    Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, Map.class);

                    // Convert JSON to Avro GenericRecord
                    GenericRecord avroRecord = jsonToAvro(jsonMap, schema);

                    // Serialize to Avro bytes
                    byte[] avroBytes = serializeAvro(avroRecord, schema);

                    // Publish to Pub/Sub
                    // Format: google-pubsub:projectId:destinationName
                    String endpoint = "google-pubsub:cogarc-notification-project:" + topic;
                    producerTemplate.sendBody(endpoint, avroBytes);

                    publishedCount++;
                    logger.info("Published message from {} (count: {})", jsonFileName, publishedCount);
                } catch (Exception e) {
                    logger.error("Failed to publish message from file: {}", jsonFileName, e);
                }
            }

            logger.info("Batch publishing completed. Published {} messages of type {}", publishedCount, messageType);
        } catch (Exception e) {
            logger.error("Error during batch publishing", e);
        }

        return publishedCount;
    }

    private List<String> findJsonFilesInClasspath(String directory) {
        List<String> fileNames = new ArrayList<>();
        try {
            // Convert directory path to classpath resource path
            String resourcePath = directory;
            // Remove src/main/resources prefix if present
            if (resourcePath.startsWith("src/main/resources/")) {
                resourcePath = resourcePath.substring("src/main/resources/".length());
            }
            // Ensure it starts with / for absolute classpath path
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }
            // Ensure it ends with / for directory
            String basePath = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
            
            logger.info("Looking for JSON files in classpath: {}", basePath);
            
            // Try to get the directory as a resource
            URL resourceUrl = getClass().getResource(basePath);
            
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                // It's a file system path (during development)
                try {
                    File dir = new File(resourceUrl.toURI());
                    logger.info("Found directory in file system: {}", dir.getAbsolutePath());
                    if (dir.exists() && dir.isDirectory()) {
                        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".json"));
                        if (files != null) {
                            for (File file : files) {
                                fileNames.add(file.getName());
                            }
                            logger.info("Found {} JSON files in file system: {}", fileNames.size(), dir.getAbsolutePath());
                        } else {
                            logger.warn("No JSON files found in directory: {}", dir.getAbsolutePath());
                        }
                    } else {
                        logger.warn("Directory does not exist or is not a directory: {}", dir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.error("Error accessing file system resource: {}", e.getMessage(), e);
                }
            } else if (resourceUrl != null && "jar".equals(resourceUrl.getProtocol())) {
                // It's in a JAR
                String jarBasePath = basePath.substring(1); // Remove leading /
                logger.info("Found JAR resource, searching in: {}", jarBasePath);
                JarURLConnection jarConnection = (java.net.JarURLConnection) resourceUrl.openConnection();
                try (JarFile jarFile = jarConnection.getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (entryName.startsWith(jarBasePath) && entryName.endsWith(".json") && !entry.isDirectory()) {
                            String fileName = entryName.substring(jarBasePath.length());
                            fileNames.add(fileName);
                        }
                    }
                    logger.info("Found {} JSON files in JAR: {}", fileNames.size(), jarBasePath);
                }
            } else {
                // Resource not found, try alternative approach - list all resources
                logger.warn("Resource URL is null or unsupported protocol for path: {}. Trying alternative approach.", basePath);
                String searchPath = basePath.substring(1); // Remove leading /
                Enumeration<URL> resources = getClass().getClassLoader().getResources(searchPath);
                if (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    if ("file".equals(url.getProtocol())) {
                        File dir = new File(url.toURI());
                        if (dir.exists() && dir.isDirectory()) {
                            File[] files = dir.listFiles((dir1, name) -> name.endsWith(".json"));
                            if (files != null) {
                                for (File file : files) {
                                    fileNames.add(file.getName());
                                }
                                logger.info("Found {} JSON files using alternative approach: {}", fileNames.size(), dir.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not load from classpath: {}", e.getMessage(), e);
        }
        return fileNames;
    }

    private String loadFileContent(String directory, String fileName) throws IOException {
        // Try classpath first
        String resourcePath = directory;
        // Remove src/main/resources prefix if present
        if (resourcePath.startsWith("src/main/resources/")) {
            resourcePath = resourcePath.substring("src/main/resources/".length());
        }
        // Ensure it starts with / for absolute classpath path
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }
        // Ensure it ends with / for directory
        if (!resourcePath.endsWith("/")) {
            resourcePath = resourcePath + "/";
        }
        resourcePath = resourcePath + fileName;
        
        logger.debug("Loading file from classpath: {}", resourcePath);
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream != null) {
            try (inputStream) {
                return new String(inputStream.readAllBytes());
            }
        }
        
        // Fallback to file system
        Path filePath = Paths.get(directory, fileName);
        if (Files.exists(filePath)) {
            logger.debug("Loading file from file system: {}", filePath);
            return Files.readString(filePath);
        }
        
        throw new IOException("File not found: " + fileName + " in " + directory);
    }

    private GenericRecord jsonToAvro(Map<String, Object> jsonMap, Schema schema) {
        GenericRecord record = new GenericData.Record(schema);
        for (Schema.Field field : schema.getFields()) {
            Object value = jsonMap.get(field.name());
            if (value == null) {
                value = field.defaultVal();
            }
            record.put(field.name(), value);
        }
        return record;
    }

    private byte[] serializeAvro(GenericRecord record, Schema schema) throws IOException {
        org.apache.avro.io.BinaryEncoder encoder = org.apache.avro.io.EncoderFactory.get()
            .binaryEncoder(new java.io.ByteArrayOutputStream(), null);
        org.apache.avro.io.DatumWriter<GenericRecord> writer = new org.apache.avro.generic.GenericDatumWriter<>(schema);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        encoder = org.apache.avro.io.EncoderFactory.get().binaryEncoder(baos, encoder);
        writer.write(record, encoder);
        encoder.flush();
        return baos.toByteArray();
    }

    private String getDirectoryForType(String messageType) {
        return switch (messageType) {
            case "order-status" -> orderStatusDir;
            case "ucc" -> uccDir;
            case "tour-appointment" -> tourAppointmentDir;
            case "truckload" -> truckloadDir;
            default -> null;
        };
    }

    private String getTopicForType(String messageType) {
        return switch (messageType) {
            case "order-status" -> orderStatusTopic;
            case "ucc" -> uccTopic;
            case "tour-appointment" -> tourAppointmentTopic;
            case "truckload" -> truckloadTopic;
            default -> null;
        };
    }
}

