package com.pa.evs.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.pa.evs.model.CARequestLog;

public class CsvUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvUtils.class);
    public static final String EXPORT_TEMP = System.getProperty("user.home") + '/' + "ca-request-log";
    
    static {
        try {
            Files.createDirectories(Paths.get(EXPORT_TEMP));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static File writeCaRequestLogCsv(List<CARequestLog> listInput, String fileName) throws IOException{

        listInput = listInput.stream().filter(input -> !input.getUid().equals("server.csr")).collect(Collectors.toList());
        List<String> headers = Arrays.asList(
                "SN", "UUID", "CID",
                "Activated date");
        return toCsv(headers, listInput, CsvUtils::toCSVRecord, buildPathFile(fileName));
    }
    
    private static List<String> toCSVRecord(int idx, CARequestLog caRequestLog) {
        List<String> record = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        
        record.add(caRequestLog.getSn());
        record.add(caRequestLog.getUid());
        record.add(caRequestLog.getCid());
        record.add(caRequestLog.getActivateDate() != null ? sdf.format(new Date(caRequestLog.getActivateDate())) : "########");
        
        return postProcessCsv(record);
    }
    
    private static String buildPathFile(String fileName) throws IOException {
        String parentFolder = (CsvUtils.EXPORT_TEMP + '/' + System.currentTimeMillis() + Math.random()).replace(".", "");
        Files.createDirectories(Paths.get(parentFolder));
        return parentFolder + '/' + fileName;
    }
    
    private static List<String> postProcessCsv(List<String> record) {
        List<String> escaped = new ArrayList<>(record.size());

        for (String column : record) {
            escaped.add(StringUtils.trimToEmpty(column));
        }

        return escaped;
    }
    
    public static <T> File toCsv(List<String> headers, List<T> items, CsvRecordConverter<T> converter, String filePath)
            throws IOException {
        String[] headersArr = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            headersArr[i] = '\ufeff' + headers.get(i);
        }
        File f = new File(filePath);
        Writer fileWriter = new FileWriter(f);
        try (
                BufferedWriter writer = new BufferedWriter(fileWriter);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withDelimiter(',')
                        .withIgnoreHeaderCase()
                        .withTrim()
                        .withHeader(headersArr));
        ) {
            if (!CollectionUtils.isEmpty(items)) {
                int idx = 0;
                for (T item : items) {
                    List<String> record = converter.toCSVRecord(idx, item);
                    for (int i = 0; i < record.size(); i++) {
                        record.set(i, '\ufeff' + record.get(i));
                    }
                    csvPrinter.printRecord(record);
                    idx++;
                }
            }
            csvPrinter.flush();
            writer.flush();
            fileWriter.flush();
            fileWriter.close();
        }
        return f;
    }
    
    @FunctionalInterface
    public interface CsvRecordConverter<T> {
        List<String> toCSVRecord(int idx, T record);
    }
}
