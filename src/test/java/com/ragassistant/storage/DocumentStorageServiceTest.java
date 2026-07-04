package com.ragassistant.storage;

import com.ragassistant.common.exceptions.RagException;
import com.ragassistant.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private DocumentStorageService service() {
        AppProperties props = new AppProperties();
        props.getStorage().setPath(tempDir.toString());
        return new DocumentStorageService(props);
    }

    @Test
    void storeLoadDelete_roundTrip() {
        DocumentStorageService service = service();
        String path = service.store("tenant1", "report.pdf", new ByteArrayInputStream("hello world".getBytes()));
        assertThat(path).contains("tenant1");

        try (InputStream in = service.load(path)) {
            String content = new String(in.readAllBytes());
            assertThat(content).isEqualTo("hello world");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        service.delete(path);
        assertThat(Files.exists(Path.of(path))).isFalse();
    }

    @Test
    void store_sanitizesUnsafeFileName() {
        DocumentStorageService service = service();
        String path = service.store("t", "../../evil name#.pdf", new ByteArrayInputStream("x".getBytes()));
        assertThat(Path.of(path).startsWith(tempDir.toAbsolutePath())).isTrue();
        assertThat(path).contains("evil");
        assertThat(Files.exists(Path.of(path))).isTrue();
    }

    @Test
    void load_missingPath_throwsRagException() {
        DocumentStorageService service = service();
        assertThatThrownBy(() -> service.load("no/such/file.pdf")).isInstanceOf(RagException.class);
    }

    @Test
    void delete_existing_removesFile() {
        DocumentStorageService service = service();
        String path = service.store("t", "a.pdf", new ByteArrayInputStream("x".getBytes()));
        service.delete(path);
        assertThat(Files.exists(Path.of(path))).isFalse();
    }

    @Test
    void delete_missingPath_isNoOp() {
        DocumentStorageService service = service();
        service.delete("no/such/file.pdf");
    }

    @Test
    void store_ioError_throwsRagException(@TempDir Path dir) throws Exception {
        Path lock = dir.resolve("lockfile");
        Files.createFile(lock);
        AppProperties props = new AppProperties();
        props.getStorage().setPath(lock.toString());
        DocumentStorageService service = new DocumentStorageService(props);

        assertThatThrownBy(() -> service.store("t", "a.txt", new ByteArrayInputStream("x".getBytes())))
                .isInstanceOf(RagException.class);
    }
}
