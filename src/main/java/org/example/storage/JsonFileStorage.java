package org.example.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * JSON 파일을 단순 영속 저장소로 활용하는 제네릭 유틸리티.
 * 소규모 POC / 설정 저장 용도에 적합.
 */
public class JsonFileStorage<T> {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final File storageFile;
    private final TypeReference<List<T>> listTypeRef;

    public JsonFileStorage(String filePath, TypeReference<List<T>> listTypeRef) {
        this.storageFile = new File(filePath);
        this.listTypeRef = listTypeRef;
    }

    /** 파일에서 전체 목록 읽기. 파일 없거나 비어있으면 빈 리스트 반환. */
    public List<T> findAll() throws IOException {
        if (!storageFile.exists() || storageFile.length() == 0) {
            return new ArrayList<>();
        }
        return mapper.readValue(storageFile, listTypeRef);
    }

    /** 조건에 맞는 항목 조회 */
    public List<T> findBy(Predicate<T> predicate) throws IOException {
        return findAll().stream().filter(predicate).toList();
    }

    /** 전체 목록을 파일에 저장 (덮어쓰기) */
    public void saveAll(List<T> items) throws IOException {
        storageFile.getParentFile().mkdirs();
        mapper.writeValue(storageFile, items);
    }

    /** 단일 항목 추가 */
    public void append(T item) throws IOException {
        List<T> items = findAll();
        items.add(item);
        saveAll(items);
    }

    /** 저장된 항목 수 */
    public int count() throws IOException {
        return findAll().size();
    }

    /** 스토리지 파일 삭제 */
    public boolean clear() {
        return storageFile.delete();
    }

    public File getStorageFile() {
        return storageFile;
    }
}
