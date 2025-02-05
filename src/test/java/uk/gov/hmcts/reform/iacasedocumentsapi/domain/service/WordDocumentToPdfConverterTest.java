package uk.gov.hmcts.reform.iacasedocumentsapi.domain.service;

import static java.nio.file.Files.readAllBytes;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.FileType;
import uk.gov.hmcts.reform.iacasedocumentsapi.infrastructure.clients.DocmosisDocumentConversionClient;

@RunWith(MockitoJUnitRunner.class)
public class WordDocumentToPdfConverterTest {

    @Mock
    private DocmosisDocumentConversionClient docmosisDocumentConversionClient;
    @Mock Resource resource;
    @Mock IOException ioException;
    @Mock InputStream inputStream;

    private WordDocumentToPdfConverter wordDocumentToPdfConverter;
    private ClassLoader classLoader = getClass().getClassLoader();

    @Before
    public void setUp() throws IOException {

        when(resource.getInputStream()).thenReturn(inputStream);

        wordDocumentToPdfConverter = new WordDocumentToPdfConverter(
            docmosisDocumentConversionClient);
    }

    @Test
    public void handles_ioexception() throws IOException {

        when(resource.getInputStream()).thenThrow(ioException);

        assertThatThrownBy(() -> wordDocumentToPdfConverter.convertResourceToPdf(resource))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to convert the document to a pdf")
            .hasCause(ioException);
    }

    @Test
    public void handles_docx_files() throws IOException {

        File docxFile = new File(
            classLoader.getResource(
                "draft-doc.docx").getPath());

        ByteArrayResource byteArrayResource =
            getByteArrayResource(
                docxFile,
                "some-word-doc.docx");

        byte[] convertedBytes = someRandomBytes();

        when(docmosisDocumentConversionClient.convert(
            any(File.class),
            eq(FileType.PDF))).thenReturn(convertedBytes);


        File pdf = wordDocumentToPdfConverter.convertResourceToPdf(byteArrayResource);

        assertThat(readAllBytes(pdf.toPath()))
            .isEqualTo(convertedBytes);
    }

    @Test
    public void convertsDocToPdf() throws IOException {

        File docFile = new File(
            classLoader.getResource(
                "draft-doc.doc").getPath());

        ByteArrayResource byteArrayResource =
            getByteArrayResource(
                docFile,
                "some-word-doc.doc");

        byte[] convertedBytes = someRandomBytes();

        when(docmosisDocumentConversionClient.convert(
            any(File.class),
            eq(FileType.PDF))).thenReturn(convertedBytes);


        File pdf = wordDocumentToPdfConverter.convertResourceToPdf(byteArrayResource);

        assertThat(readAllBytes(pdf.toPath()))
            .isEqualTo(convertedBytes);
    }

    private ByteArrayResource getByteArrayResource(
        File file,
        String filename
    ) {
        byte[] byteArray = new byte[0];

        try {
            byteArray = readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ByteArrayResource(byteArray) {

            @Override
            public File getFile() {
                return file;
            }

            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private byte[] someRandomBytes() {

        byte[] b = new byte[20];
        new Random().nextBytes(b);

        return b;
    }
}