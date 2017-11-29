package tech.nikolaev.fias.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by andrey.l.nikolaev@mail.ru on 9/17/17.
 */
public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static boolean checkHttpStatus(int status) {
        return (status >= 200 && status < 300);
    }

    private HttpUtils() {
    }

    public static Path downloadFileToTemp(String url) throws IOException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("fias", "xml");
            downloadFile(url, tempFile);
            return tempFile;
        } catch (IOException e) {
            FileUtils.deleteFile(tempFile);
            throw e;
        }
    }

    public static boolean checkResourceExists(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get);
        ) {
            Integer status = response.getStatusLine().getStatusCode();
            return checkHttpStatus(status);
        } catch (IOException ioe) {
            logger.warn("Error check resource '{}'", url, ioe);
            throw ioe;
        }
    }

    public static void downloadToStream(String url, OutputStream os) throws IOException {
        logger.debug("Download resource '{}'", url);
        int status = -1;
        HttpGet get = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get);
        ) {
            status = response.getStatusLine().getStatusCode();

            if (!checkHttpStatus(status)) {
                throw new IOException("Error download file  from '" + url + "': " + status);
            }

            InputStream is = null;
            try {
                is = response.getEntity().getContent();
                StreamUtils.copy(is, os);
            } finally {
                FileUtils.close(is);
            }
            logger.debug("Download '{}' is completed", url);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            throw ioe;
        }
    }

    public static void downloadFile(String url, Path targetFile) throws IOException {
            try (OutputStream os = new FileOutputStream(targetFile.toFile(), false)) {
                downloadToStream(url, os);
            } catch (IOException ioe) {
                logger.error(ioe.getMessage());
                throw ioe;
            }
    }

    public static byte[] downloadToArray(String url) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            downloadToStream(url, os);
            return os.toByteArray();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            throw ioe;
        }
    }




}
