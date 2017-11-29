package tech.nikolaev.fias.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;
import tech.nikolaev.fias.exception.FiasException;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by andrey.l.nikolaev@mail.ru on 17.08.2017.
 */
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);


    private FileUtils() {
    }

    public static void close(OutputStream os) {
        if (null != os) {
            try {
                os.close();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static void close(InputStream is) {
        if (null != is) {
            try {
                is.close();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static void deleteFile(Path path) {
        try {
            logger.debug("delete file: {}", path);
            if (!path.toFile().delete()) {
                throw new IOException("Error delete file: " + path);
            }
        } catch (IOException e) {
            logger.warn("Error delete file: '{}'", path);
        }
    }

    public static void delete(Path path) {
        if (null != path && path.toFile().exists()) {
            File file = path.toFile();
            if (file.isFile()) {
                deleteFile( path);
            } else if (file.isDirectory()) {
                logger.debug("delete directory: {}", path);
                FileSystemUtils.deleteRecursively(file);
            }
        }
    }


    public static List<String> unzipToDir(Path zipfile, Path targetDirectory) throws FiasException {
        ZipInputStream zis = null;
        BufferedOutputStream bos = null;
        List<String> fileNames = new ArrayList<>();
        byte[] buffer = new byte[1024*1024];
        try {
            logger.debug("Start unzipping file '{}' to '{}'", zipfile.getFileName(), targetDirectory);

            File targetDir = targetDirectory.toFile();
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            zis = new ZipInputStream(new FileInputStream(zipfile.toFile()));

            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                fileNames.add(zipEntry.getName());

                String fileName = zipEntry.getName();
                File entryFile = new File(targetDirectory.toAbsolutePath() + File.separator + fileName);
                new File(entryFile.getParent()).mkdirs();
                try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            logger.debug("Unzipping file '{}' to '{}' is finished", zipfile, targetDirectory);
        } catch (IOException io) {
            throw new FiasException("Error unzip file: '" + zipfile.getFileName() + "', error: " + io.getMessage(), io);
        } finally {
            close(bos);
            close(zis);
        }
        return fileNames;
    }

}
