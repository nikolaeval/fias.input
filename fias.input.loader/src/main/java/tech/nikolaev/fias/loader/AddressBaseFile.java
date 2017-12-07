package tech.nikolaev.fias.loader;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.ArchiveFileNotFoundException;
import tech.nikolaev.fias.exception.FiasException;
import tech.nikolaev.fias.util.FileUtils;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Service
@Scope("prototype")
public class AddressBaseFile {

    private static final Logger logger = LoggerFactory.getLogger(AddressBaseFile.class);

    private Path filepath;

    public AddressBaseFile(Path filepath) {
        this.filepath = filepath;
    }

    private FileHeader getArchiveHeader(Archive archive, String filename) throws FiasException {
        final int FILENAME_SPLITTER_POS = 7;
        List<FileHeader> list = archive.getFileHeaders();
        for (FileHeader h : list) {
            if (h.isDirectory()) continue;
            int n = h.getFileNameString().indexOf('_', FILENAME_SPLITTER_POS);
            if (-1 != n && h.getFileNameString().substring(0, n).equals(filename)) {
                return h;
            }
        }
        throw new ArchiveFileNotFoundException("File '" + filename + "' is not found in archive");
    }


    public void processFile(String filename, DataLoader loader) throws FiasException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Archive archive = new Archive(filepath.toFile());
            PipedInputStream is = new PipedInputStream();
            PipedOutputStream os = new PipedOutputStream(is)) {
            FileHeader fileHeader = getArchiveHeader(archive, filename);
            logger.info("Processing file: '{}'", filename);
            Future future = executor.submit(() -> loader.loadData(is));
            archive.extractFile(fileHeader, os);
            FileUtils.close(os);
            future.get();
        } catch (ExecutionException | InterruptedException | IOException | RarException e) {
            logger.error("Processing file '{}' error: {}", filename, e.getMessage());
            logger.error(e.getMessage(), e);
            throw new FiasException(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }

    }


}
